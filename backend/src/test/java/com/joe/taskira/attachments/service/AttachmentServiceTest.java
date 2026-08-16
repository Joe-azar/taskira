package com.joe.taskira.attachments.service;

import com.joe.taskira.attachments.dto.AttachmentResponse;
import com.joe.taskira.attachments.entity.Attachment;
import com.joe.taskira.attachments.port.DocumentStorage;
import com.joe.taskira.attachments.repository.AttachmentRepository;
import com.joe.taskira.audit.service.AuditService;
import com.joe.taskira.common.exception.ConflictException;
import com.joe.taskira.common.exception.ForbiddenException;
import com.joe.taskira.project.entity.Project;
import com.joe.taskira.project.enums.ProjectStatus;
import com.joe.taskira.project.repository.ProjectMemberRepository;
import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.repository.TicketRepository;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentStorage documentStorage;

    @Mock
    private AuditService auditService;

    private AttachmentService attachmentService;

    private User member;
    private Project project;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        attachmentService = new AttachmentService(
                attachmentRepository, ticketRepository, projectMemberRepository, userRepository, documentStorage, auditService
        );
        ReflectionTestUtils.setField(attachmentService, "maxSizeBytes", 20_971_520L);

        member = User.builder().firstName("A").lastName("B").email("member@taskira.test")
                .passwordHash("x").globalRole(GlobalRole.USER).active(true).build();
        ReflectionTestUtils.setField(member, "id", 1L);

        User owner = User.builder().firstName("O").lastName("W").email("owner@taskira.test")
                .passwordHash("x").globalRole(GlobalRole.USER).active(true).build();
        ReflectionTestUtils.setField(owner, "id", 2L);

        project = Project.builder().code("PROJ").name("Project").owner(owner)
                .status(ProjectStatus.ACTIVE).ticketSequence(0).build();
        ReflectionTestUtils.setField(project, "id", 100L);

        ticket = Ticket.builder().reference("PROJ-1").project(project).title("Ticket")
                .type(com.joe.taskira.ticket.enums.TicketType.TASK).status(com.joe.taskira.ticket.enums.TicketStatus.OPEN)
                .priority(com.joe.taskira.ticket.enums.TicketPriority.MEDIUM).creator(owner).build();
        ReflectionTestUtils.setField(ticket, "id", 10L);

        AuthenticatedUser principal = new AuthenticatedUser(member);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadRejectsAFileWhoseRealContentIsNotOnTheAllowlistEvenIfNamedLikeAnImage() throws Exception {
        lenient().when(ticketRepository.findByIdWithRelations(10L)).thenReturn(Optional.of(ticket));
        lenient().when(projectMemberRepository.existsByProjectIdAndUserId(100L, 1L)).thenReturn(true);

        // A shell script disguised with a .png filename - Tika sniffs the real content,
        // not the extension or the client-declared Content-Type.
        MockMultipartFile disguised = new MockMultipartFile(
                "file", "totally-a-photo.png", "image/png",
                "#!/bin/sh\necho pwned\n".getBytes()
        );

        assertThatThrownBy(() -> attachmentService.uploadAttachment(10L, disguised))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not allowed");

        verify(documentStorage, never()).store(any());
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void uploadOfARealAllowlistedFileStoresItAndComputesARealSha256() throws Exception {
        when(ticketRepository.findByIdWithRelations(10L)).thenReturn(Optional.of(ticket));
        when(projectMemberRepository.existsByProjectIdAndUserId(100L, 1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(member));
        when(documentStorage.store(any())).thenReturn("generated-key");

        byte[] content = "plain text attachment content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", content);

        Attachment saved = Attachment.builder()
                .ticket(ticket).uploader(member).originalFilename("notes.txt")
                .contentType("text/plain").sizeBytes(content.length).sha256("irrelevant-for-this-stub")
                .storageKey("generated-key").build();
        ReflectionTestUtils.setField(saved, "id", 500L);
        when(attachmentRepository.save(any())).thenReturn(saved);
        when(attachmentRepository.findByIdWithRelations(500L)).thenReturn(Optional.of(saved));

        AttachmentResponse response = attachmentService.uploadAttachment(10L, file);

        assertThat(response.originalFilename()).isEqualTo("notes.txt");
        assertThat(response.contentType()).isEqualTo("text/plain");

        ArgumentCaptor<Attachment> captor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).save(captor.capture());
        // SHA-256 of "plain text attachment content", computed independently to prove
        // the service isn't just storing a placeholder.
        assertThat(captor.getValue().getSha256())
                .isEqualTo(sha256Hex(content))
                .hasSize(64);
    }

    @Test
    void uploadRejectsAFileLargerThanTheConfiguredLimit() {
        lenient().when(ticketRepository.findByIdWithRelations(10L)).thenReturn(Optional.of(ticket));
        lenient().when(projectMemberRepository.existsByProjectIdAndUserId(100L, 1L)).thenReturn(true);
        ReflectionTestUtils.setField(attachmentService, "maxSizeBytes", 5L);

        MockMultipartFile file = new MockMultipartFile("file", "big.txt", "text/plain", "this is more than five bytes".getBytes());

        assertThatThrownBy(() -> attachmentService.uploadAttachment(10L, file))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void uploadRejectsAnEmptyFile() {
        lenient().when(ticketRepository.findByIdWithRelations(10L)).thenReturn(Optional.of(ticket));
        lenient().when(projectMemberRepository.existsByProjectIdAndUserId(100L, 1L)).thenReturn(true);

        MockMultipartFile empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> attachmentService.uploadAttachment(10L, empty))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void uploadRejectsAnArchivedProjectEvenForAnOtherwiseAuthorizedMember() {
        project.setStatus(ProjectStatus.ARCHIVED);
        lenient().when(ticketRepository.findByIdWithRelations(10L)).thenReturn(Optional.of(ticket));
        lenient().when(projectMemberRepository.existsByProjectIdAndUserId(100L, 1L)).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "content".getBytes());

        assertThatThrownBy(() -> attachmentService.uploadAttachment(10L, file))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("archived");
    }

    @Test
    void uploadRejectsANonMemberOfTheProject() {
        when(ticketRepository.findByIdWithRelations(10L)).thenReturn(Optional.of(ticket));
        when(projectMemberRepository.existsByProjectIdAndUserId(100L, 1L)).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "content".getBytes());

        assertThatThrownBy(() -> attachmentService.uploadAttachment(10L, file))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void filenamesAreSanitizedAgainstPathSeparatorsAndControlCharactersBeforeBeingStored() throws Exception {
        when(ticketRepository.findByIdWithRelations(10L)).thenReturn(Optional.of(ticket));
        when(projectMemberRepository.existsByProjectIdAndUserId(100L, 1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(member));
        when(documentStorage.store(any())).thenReturn("generated-key");
        when(attachmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(attachmentRepository.findByIdWithRelations(any())).thenAnswer(invocation -> {
            Attachment a = Attachment.builder().ticket(ticket).uploader(member)
                    .originalFilename("etc_passwd").contentType("text/plain")
                    .sizeBytes(1).sha256("x").storageKey("generated-key").build();
            return Optional.of(a);
        });

        MockMultipartFile maliciousName = new MockMultipartFile(
                "file", "../../etc/passwd", "text/plain", "x".getBytes()
        );

        attachmentService.uploadAttachment(10L, maliciousName);

        ArgumentCaptor<Attachment> captor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).save(captor.capture());
        assertThat(captor.getValue().getOriginalFilename()).doesNotContain("/").doesNotContain("\\");
    }

    private static String sha256Hex(byte[] content) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        return java.util.HexFormat.of().formatHex(digest.digest(content));
    }
}
