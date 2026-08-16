package com.joe.taskira.attachments.service;

import com.joe.taskira.attachments.dto.AttachmentResponse;
import com.joe.taskira.attachments.entity.Attachment;
import com.joe.taskira.attachments.port.DocumentStorage;
import com.joe.taskira.attachments.repository.AttachmentRepository;
import com.joe.taskira.audit.enums.AuditAction;
import com.joe.taskira.audit.enums.AuditEntityType;
import com.joe.taskira.audit.service.AuditService;
import com.joe.taskira.common.exception.ConflictException;
import com.joe.taskira.common.exception.ForbiddenException;
import com.joe.taskira.common.exception.ResourceNotFoundException;
import com.joe.taskira.common.util.SecurityUtils;
import com.joe.taskira.project.entity.Project;
import com.joe.taskira.project.entity.ProjectMember;
import com.joe.taskira.project.enums.ProjectRole;
import com.joe.taskira.project.enums.ProjectStatus;
import com.joe.taskira.project.repository.ProjectMemberRepository;
import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.repository.TicketRepository;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AttachmentService {

    // Explicit allowlist, not a denylist - anything not recognized as one of these
    // real, Tika-detected content types is rejected, including every kind of
    // executable or script regardless of how the client labeled it. See ADR-0021.
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp",
            "application/pdf",
            "text/plain",
            "application/zip",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final DocumentStorage documentStorage;
    private final AuditService auditService;
    private final Tika tika = new Tika();

    @Value("${app.attachments.max-size-bytes}")
    private long maxSizeBytes;

    public record AttachmentDownload(String originalFilename, String contentType, long sizeBytes, String storageKey) {
    }

    public AttachmentResponse uploadAttachment(Long ticketId, MultipartFile file) {
        Ticket ticket = findTicketOrThrow(ticketId);
        assertCanAccessProject(ticket.getProject());

        if (ticket.getProject().getStatus() == ProjectStatus.ARCHIVED) {
            throw new ConflictException("Cannot add attachments in archived projects");
        }

        if (file.isEmpty()) {
            throw new ConflictException("File is empty");
        }

        if (file.getSize() > maxSizeBytes) {
            throw new ConflictException("File exceeds the maximum allowed size of " + maxSizeBytes + " bytes");
        }

        // Content-type rejection doesn't need to know who's uploading - fail fast on a
        // disallowed file before spending a lookup on the current user at all.
        String detectedContentType = detectContentType(file);
        if (!ALLOWED_CONTENT_TYPES.contains(detectedContentType)) {
            throw new ConflictException("File type not allowed: " + detectedContentType);
        }

        User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        // Hashed from the file's own bytes, not from whatever DocumentStorage.store(...)
        // happens to read off a wrapped stream: a storage adapter that doesn't fully
        // consume its input (or, as caught here, a mock that doesn't touch it at all)
        // would silently produce the hash of zero bytes instead of a real error.
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }

        String sha256;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            sha256 = HexFormat.of().formatHex(digest.digest(fileBytes));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a JDK-mandatory algorithm (JLS/JCA baseline) - unreachable in
            // practice, but MessageDigest.getInstance declares it checked.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }

        String storageKey;
        try (InputStream toStore = new java.io.ByteArrayInputStream(fileBytes)) {
            storageKey = documentStorage.store(toStore);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store attachment", e);
        }

        Attachment attachment = Attachment.builder()
                .ticket(ticket)
                .uploader(currentUser)
                .originalFilename(sanitizeFilename(file.getOriginalFilename()))
                .contentType(detectedContentType)
                .sizeBytes(file.getSize())
                .sha256(sha256)
                .storageKey(storageKey)
                .build();
        attachment = attachmentRepository.save(attachment);

        auditService.record(
                currentUser.getId(),
                currentUser.getEmail(),
                AuditEntityType.ATTACHMENT,
                attachment.getId(),
                AuditAction.ATTACHMENT_CREATED,
                attachment.getOriginalFilename()
        );

        return AttachmentResponse.from(attachmentRepository.findByIdWithRelations(attachment.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found after creation")));
    }

    @Transactional
    public List<AttachmentResponse> listTicketAttachments(Long ticketId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        assertCanAccessProject(ticket.getProject());

        return attachmentRepository.findByTicketIdWithRelations(ticketId).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @Transactional
    public AttachmentDownload prepareDownload(Long attachmentId) {
        Attachment attachment = findAttachmentOrThrow(attachmentId);
        assertCanAccessProject(attachment.getTicket().getProject());

        return new AttachmentDownload(
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getStorageKey()
        );
    }

    public void streamTo(String storageKey, OutputStream out) {
        try (InputStream in = documentStorage.retrieve(storageKey)) {
            in.transferTo(out);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to stream attachment", e);
        }
    }

    public void deleteAttachment(Long attachmentId) {
        Attachment attachment = findAttachmentOrThrow(attachmentId);
        Project project = attachment.getTicket().getProject();

        assertCanDeleteAttachment(attachment, project);

        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new ConflictException("Cannot delete attachments in archived projects");
        }

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        try {
            documentStorage.delete(attachment.getStorageKey());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete attachment content", e);
        }

        attachmentRepository.delete(attachment);

        auditService.record(
                currentUser.getId(),
                currentUser.getUser().getEmail(),
                AuditEntityType.ATTACHMENT,
                attachmentId,
                AuditAction.ATTACHMENT_DELETED,
                attachment.getOriginalFilename()
        );
    }

    private String detectContentType(MultipartFile file) {
        try (InputStream detectionStream = file.getInputStream()) {
            return tika.detect(detectionStream, file.getOriginalFilename());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to inspect uploaded file", e);
        }
    }

    // Kept only as display metadata (never used as a filesystem path - see
    // LocalFileSystemStorage), but still stripped of path separators and control
    // characters before it can reach an HTTP response header (Content-Disposition).
    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "attachment";
        }
        String noPath = originalFilename.replaceAll("[\\\\/]", "_");
        String noControlChars = noPath.replaceAll("[\\x00-\\x1F\"]", "_");
        return noControlChars.length() > 255 ? noControlChars.substring(0, 255) : noControlChars;
    }

    private Ticket findTicketOrThrow(Long ticketId) {
        return ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private Attachment findAttachmentOrThrow(Long attachmentId) {
        return attachmentRepository.findByIdWithRelations(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
    }

    private void assertCanAccessProject(Project project) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (currentUser.getUser().getGlobalRole() == GlobalRole.ADMIN) {
            return;
        }

        if (project.getOwner().getId().equals(currentUser.getId())) {
            return;
        }

        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), currentUser.getId());

        if (!isMember) {
            throw new ForbiddenException("You are not allowed to access this project");
        }
    }

    // Mirrors CommentService's delete rule: the uploader, an admin, the project owner,
    // or an OWNER/MANAGER project member can remove an attachment - not just anyone
    // with read access to the ticket.
    private void assertCanDeleteAttachment(Attachment attachment, Project project) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        boolean isUploader = attachment.getUploader().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getUser().getGlobalRole() == GlobalRole.ADMIN;
        boolean isProjectOwner = project.getOwner().getId().equals(currentUser.getId());

        if (isUploader || isAdmin || isProjectOwner) {
            return;
        }

        ProjectMember membership = projectMemberRepository
                .findByProjectIdAndUserId(project.getId(), currentUser.getId())
                .orElse(null);

        boolean canManage = membership != null
                && (membership.getProjectRole() == ProjectRole.OWNER || membership.getProjectRole() == ProjectRole.MANAGER);

        if (!canManage) {
            throw new ForbiddenException("You are not allowed to delete this attachment");
        }
    }
}
