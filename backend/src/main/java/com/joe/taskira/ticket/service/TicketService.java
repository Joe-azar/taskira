package com.joe.taskira.ticket.service;

import com.joe.taskira.common.exception.ConflictException;
import com.joe.taskira.common.exception.ForbiddenException;
import com.joe.taskira.common.exception.ResourceNotFoundException;
import com.joe.taskira.common.util.SecurityUtils;
import com.joe.taskira.project.entity.Project;
import com.joe.taskira.project.entity.ProjectMember;
import com.joe.taskira.project.enums.ProjectRole;
import com.joe.taskira.project.repository.ProjectMemberRepository;
import com.joe.taskira.project.repository.ProjectRepository;
import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.ticket.dto.*;
import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.enums.TicketPriority;
import com.joe.taskira.ticket.enums.TicketStatus;
import com.joe.taskira.ticket.repository.TicketRepository;
import com.joe.taskira.user.entity.User;
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

    public TicketResponse createTicket(CreateTicketRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Project project = projectRepository.findByIdForUpdate(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        assertCanAccessProject(project);

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
    public TicketResponse getTicketById(Long ticketId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        assertCanAccessProject(ticket.getProject());

        return TicketResponse.from(ticket);
    }

    public TicketResponse updateStatus(Long ticketId, UpdateTicketStatusRequest request) {
        Ticket ticket = findTicketOrThrow(ticketId);
        assertCanAccessProject(ticket.getProject());

        User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

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

    private boolean isUserPartOfProject(Project project, Long userId) {
        return project.getOwner().getId().equals(userId)
                || projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userId);
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}