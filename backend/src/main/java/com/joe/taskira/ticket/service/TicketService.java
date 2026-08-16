package com.joe.taskira.ticket.service;

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
import com.joe.taskira.project.repository.ProjectRepository;
import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.ticket.dto.*;
import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.enums.TicketPriority;
import com.joe.taskira.ticket.enums.TicketStatus;
import com.joe.taskira.ticket.enums.TicketType;
import com.joe.taskira.ticket.repository.TicketRepository;
import com.joe.taskira.ticket.specification.TicketSpecifications;
import com.joe.taskira.user.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TicketHistoryService ticketHistoryService;
    private final AuditService auditService;

    public TicketResponse createTicket(CreateTicketRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Project project = projectRepository.findByIdForUpdate(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        assertCanAccessProject(project);

        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new ConflictException("Cannot create tickets in archived projects");
        }

        User assignee = null;
        if (request.assigneeId() != null) {
            assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));

            if (!assignee.isActive()) {
                throw new ConflictException("Inactive user cannot be assigned");
            }

            if (!isUserPartOfProject(project, assignee.getId())) {
                throw new ConflictException("Assignee must belong to the project");
            }
        }

        int nextSequence = project.getTicketSequence() + 1;
        project.setTicketSequence(nextSequence);

        Ticket ticket = Ticket.builder()
                .reference(project.getCode() + "-" + nextSequence)
                .project(project)
                .title(request.title().trim())
                .description(normalizeDescription(request.description()))
                .type(request.type())
                .status(TicketStatus.OPEN)
                .priority(request.priority() != null ? request.priority() : TicketPriority.MEDIUM)
                .creator(currentUser)
                .assignee(assignee)
                .dueDate(request.dueDate())
                .build();

        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logChange(
                ticket,
                currentUser,
                "CREATED",
                null,
                ticket.getReference()
        );

        auditService.record(
                currentUser.getId(),
                currentUser.getEmail(),
                AuditEntityType.TICKET,
                ticket.getId(),
                AuditAction.TICKET_CREATED,
                ticket.getReference()
        );

        return TicketResponse.from(ticketRepository.findByIdWithRelations(ticket.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found after creation")));
    }

    @Transactional
    public List<TicketSummaryResponse> listProjectTickets(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        assertCanAccessProject(project);

        return ticketRepository.findByProjectIdWithRelations(projectId).stream()
                .map(TicketSummaryResponse::from)
                .toList();
    }

    @Transactional
    public List<TicketSummaryResponse> searchTickets(
            Long projectId,
            TicketStatus status,
            TicketPriority priority,
            TicketType type,
            Long creatorId,
            Long assigneeId,
            Boolean unassigned,
            String q
    ) {
        return this.searchTicketsPage(projectId, status, priority, type, creatorId, assigneeId, unassigned, q, 0, 50, "createdAt,desc")
                .content();
    }

    @Transactional
    public TicketPageResponse searchTicketsPage(
            Long projectId,
            TicketStatus status,
            TicketPriority priority,
            TicketType type,
            Long creatorId,
            Long assigneeId,
            Boolean unassigned,
            String q,
            int page,
            int size,
            String sort
    ) {
        // Secure pagination bounds
        page = Math.max(0, page);
        size = Math.min(Math.max(1, size), 100);
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        List<Long> accessibleProjectIds = currentUser.getUser().getGlobalRole() == GlobalRole.ADMIN
                ? projectRepository.findAllProjectIds()
                : projectRepository.findAccessibleProjectIds(currentUser.getId());

        if (accessibleProjectIds.isEmpty()) {
            return new TicketPageResponse(List.of(), 0, 0, 0, 0);
        }

        if (projectId != null && !accessibleProjectIds.contains(projectId)) {
            throw new ForbiddenException("You are not allowed to access this project");
        }

        Specification<Ticket> spec = Specification.where(TicketSpecifications.hasProjectIds(accessibleProjectIds))
                .and(TicketSpecifications.hasProjectId(projectId))
                .and(TicketSpecifications.hasStatus(status))
                .and(TicketSpecifications.hasPriority(priority))
                .and(TicketSpecifications.hasType(type))
                .and(TicketSpecifications.hasCreatorId(creatorId))
                .and(TicketSpecifications.hasAssigneeId(assigneeId))
                .and(TicketSpecifications.isUnassigned(unassigned))
                .and(TicketSpecifications.matchesKeyword(q));

        Sort sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && !sort.isBlank()) {
            String[] sortParts = sort.split(",");
            if (sortParts.length == 2) {
                String property = sortParts[0].trim();
                String direction = sortParts[1].trim();
                List<String> allowedProperties = List.of("id", "reference", "title", "status", "priority", "type", "createdAt", "updatedAt", "dueDate");
                if (allowedProperties.contains(property)) {
                    try {
                        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
                        sortObj = Sort.by(sortDirection, property);
                    } catch (IllegalArgumentException ignored) {
                        sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
                    }
                }
            }
        }

        var pageRequest = org.springframework.data.domain.PageRequest.of(page, size, sortObj);
        var pageResult = ticketRepository.findAll(spec, pageRequest);

        List<TicketSummaryResponse> content = pageResult.getContent().stream()
                .map(TicketSummaryResponse::from)
                .toList();

        return new TicketPageResponse(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    @Transactional
    public TicketResponse getTicketById(Long ticketId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        assertCanAccessProject(ticket.getProject());

        return TicketResponse.from(ticket);
    }

    public TicketResponse updateStatus(Long ticketId, UpdateTicketStatusRequest request) {
        Ticket ticket = findTicketOrThrow(ticketId);
        assertCanEditTicket(ticket.getProject());

        User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        validateStatusTransition(ticket.getStatus(), request.status());

        String oldStatus = ticket.getStatus().name();
        String newStatus = request.status().name();

        ticket.setStatus(request.status());

        ticketHistoryService.logChange(
                ticket,
                currentUser,
                "STATUS",
                oldStatus,
                newStatus
        );

        auditService.record(
                currentUser.getId(),
                currentUser.getEmail(),
                AuditEntityType.TICKET,
                ticket.getId(),
                AuditAction.TICKET_STATUS_CHANGED,
                oldStatus + " -> " + newStatus
        );

        return TicketResponse.from(ticket);
    }

    public TicketResponse updateAssignee(Long ticketId, UpdateTicketAssigneeRequest request) {
        Ticket ticket = findTicketOrThrow(ticketId);
        Project project = ticket.getProject();

        assertCanManageAssignments(project);

        User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        String oldAssignee = ticket.getAssignee() != null ? ticket.getAssignee().getEmail() : null;

        if (request.assigneeId() == null) {
            ticket.setAssignee(null);

            ticketHistoryService.logChange(
                    ticket,
                    currentUser,
                    "ASSIGNEE",
                    oldAssignee,
                    null
            );

            return TicketResponse.from(ticket);
        }

        User assignee = userRepository.findById(request.assigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));

        if (!assignee.isActive()) {
            throw new ConflictException("Inactive user cannot be assigned");
        }

        if (!isUserPartOfProject(project, assignee.getId())) {
            throw new ConflictException("Assignee must belong to the project");
        }

        ticket.setAssignee(assignee);

        ticketHistoryService.logChange(
                ticket,
                currentUser,
                "ASSIGNEE",
                oldAssignee,
                assignee.getEmail()
        );

        return TicketResponse.from(ticket);
    }

    public TicketResponse updateTicket(Long ticketId, UpdateTicketRequest request) {
        Ticket ticket = findTicketOrThrow(ticketId);
        Project project = ticket.getProject();

        assertCanEditTicket(project);

        User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        // Track changes for history
        boolean hasChanges = false;

        if (!ticket.getTitle().equals(request.title().trim())) {
            ticketHistoryService.logChange(
                    ticket,
                    currentUser,
                    "TITLE",
                    ticket.getTitle(),
                    request.title().trim()
            );
            ticket.setTitle(request.title().trim());
            hasChanges = true;
        }

        String normalizedDescription = normalizeDescription(request.description());
        if (!java.util.Objects.equals(ticket.getDescription(), normalizedDescription)) {
            ticketHistoryService.logChange(
                    ticket,
                    currentUser,
                    "DESCRIPTION",
                    ticket.getDescription(),
                    normalizedDescription
            );
            ticket.setDescription(normalizedDescription);
            hasChanges = true;
        }

        if (ticket.getType() != request.type()) {
            ticketHistoryService.logChange(
                    ticket,
                    currentUser,
                    "TYPE",
                    ticket.getType().name(),
                    request.type().name()
            );
            ticket.setType(request.type());
            hasChanges = true;
        }

        if (ticket.getPriority() != request.priority()) {
            ticketHistoryService.logChange(
                    ticket,
                    currentUser,
                    "PRIORITY",
                    ticket.getPriority().name(),
                    request.priority().name()
            );
            ticket.setPriority(request.priority());
            hasChanges = true;
        }

        if (!java.util.Objects.equals(ticket.getDueDate(), request.dueDate())) {
            ticketHistoryService.logChange(
                    ticket,
                    currentUser,
                    "DUE_DATE",
                    ticket.getDueDate() != null ? ticket.getDueDate().toString() : null,
                    request.dueDate() != null ? request.dueDate().toString() : null
            );
            ticket.setDueDate(request.dueDate());
            hasChanges = true;
        }

        if (hasChanges) {
            ticket = ticketRepository.save(ticket);
        }

        return TicketResponse.from(ticket);
    }

    private Ticket findTicketOrThrow(Long ticketId) {
        return ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
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

    private void assertCanManageAssignments(Project project) {
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new ConflictException("Cannot modify tickets in archived projects");
        }

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (currentUser.getUser().getGlobalRole() == GlobalRole.ADMIN) {
            return;
        }

        if (project.getOwner().getId().equals(currentUser.getId())) {
            return;
        }

        ProjectMember membership = projectMemberRepository.findByProjectIdAndUserId(project.getId(), currentUser.getId())
                .orElseThrow(() -> new ForbiddenException("You are not allowed to manage assignments"));

        if (membership.getProjectRole() != ProjectRole.OWNER && membership.getProjectRole() != ProjectRole.MANAGER) {
            throw new ForbiddenException("You are not allowed to manage assignments");
        }
    }

    private void assertCanEditTicket(Project project) {
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new ConflictException("Cannot modify tickets in archived projects");
        }

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (currentUser.getUser().getGlobalRole() == GlobalRole.ADMIN) {
            return;
        }

        if (project.getOwner().getId().equals(currentUser.getId())) {
            return;
        }

        ProjectMember membership = projectMemberRepository.findByProjectIdAndUserId(project.getId(), currentUser.getId())
                .orElseThrow(() -> new ForbiddenException("You are not allowed to edit tickets"));

        if (membership.getProjectRole() != ProjectRole.OWNER && membership.getProjectRole() != ProjectRole.MANAGER) {
            throw new ForbiddenException("You are not allowed to edit tickets");
        }
    }

    private boolean isUserPartOfProject(Project project, Long userId) {
        return project.getOwner().getId().equals(userId)
                || projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userId);
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private void validateStatusTransition(TicketStatus currentStatus, TicketStatus newStatus) {
        if (currentStatus == newStatus) {
            throw new ConflictException("Ticket is already in status " + newStatus);
        }

        switch (currentStatus) {
            case OPEN:
                if (newStatus != TicketStatus.IN_PROGRESS && newStatus != TicketStatus.CANCELLED) {
                    throw new ConflictException("Cannot change status from OPEN to " + newStatus);
                }
                break;
            case IN_PROGRESS:
                if (newStatus != TicketStatus.OPEN && newStatus != TicketStatus.REVIEW && newStatus != TicketStatus.DONE && newStatus != TicketStatus.CANCELLED && newStatus != TicketStatus.BLOCKED) {
                    throw new ConflictException("Cannot change status from IN_PROGRESS to " + newStatus);
                }
                break;
            case REVIEW:
                if (newStatus != TicketStatus.IN_PROGRESS && newStatus != TicketStatus.DONE && newStatus != TicketStatus.CANCELLED) {
                    throw new ConflictException("Cannot change status from REVIEW to " + newStatus);
                }
                break;
            case DONE:
                // DONE is terminal, no transitions allowed except possibly reopening, but for now, none
                throw new ConflictException("Cannot change status from DONE");
            case BLOCKED:
                if (newStatus != TicketStatus.OPEN && newStatus != TicketStatus.IN_PROGRESS && newStatus != TicketStatus.CANCELLED) {
                    throw new ConflictException("Cannot change status from BLOCKED to " + newStatus);
                }
                break;
            case CANCELLED:
                // CANCELLED is terminal, but allow reopening to OPEN
                if (newStatus != TicketStatus.OPEN) {
                    throw new ConflictException("Cannot change status from CANCELLED to " + newStatus + ". Only reopening to OPEN is allowed.");
                }
                break;
        }
    }
}