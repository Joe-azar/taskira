package com.joe.taskira.ticket.service;

import com.joe.taskira.project.service.ProjectMemberAssignmentCheck;
import com.joe.taskira.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Ticket module's implementation of the project module's
 * {@link ProjectMemberAssignmentCheck} port. See ADR-0016.
 */
@Component
@RequiredArgsConstructor
class ProjectMemberAssignmentCheckAdapter implements ProjectMemberAssignmentCheck {

    private final TicketRepository ticketRepository;

    @Override
    public long countAssignedTickets(Long projectId, Long userId) {
        return ticketRepository.countByProjectIdAndAssigneeId(projectId, userId);
    }
}
