package com.joe.taskira.ticket.service;

import com.joe.taskira.common.exception.ForbiddenException;
import com.joe.taskira.common.exception.ResourceNotFoundException;
import com.joe.taskira.common.util.SecurityUtils;
import com.joe.taskira.project.entity.Project;
import com.joe.taskira.project.repository.ProjectMemberRepository;
import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.ticket.dto.TicketHistoryResponse;
import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.entity.TicketHistory;
import com.joe.taskira.ticket.repository.TicketHistoryRepository;
import com.joe.taskira.ticket.repository.TicketRepository;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketHistoryService {

    private final TicketHistoryRepository ticketHistoryRepository;
    private final TicketRepository ticketRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public void logChange(Ticket ticket, User changedBy, String fieldName, String oldValue, String newValue) {
        TicketHistory history = TicketHistory.builder()
                .ticket(ticket)
                .changedBy(changedBy)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        ticketHistoryRepository.save(history);
    }

    @Transactional
    public List<TicketHistoryResponse> listTicketHistory(Long ticketId) {
        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        assertCanAccessProject(ticket.getProject());

        return ticketHistoryRepository.findByTicketIdWithRelations(ticketId).stream()
                .map(TicketHistoryResponse::from)
                .toList();
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
}