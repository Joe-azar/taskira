package com.joe.taskira.dashboard.service;

import com.joe.taskira.common.util.SecurityUtils;
import com.joe.taskira.dashboard.dto.DashboardSummaryResponse;
import com.joe.taskira.project.repository.ProjectRepository;
import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.ticket.dto.TicketSummaryResponse;
import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.enums.TicketPriority;
import com.joe.taskira.ticket.enums.TicketStatus;
import com.joe.taskira.ticket.repository.TicketRepository;
import com.joe.taskira.ticket.specification.TicketSpecifications;
import com.joe.taskira.user.enums.GlobalRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final TicketRepository ticketRepository;

    public DashboardSummaryResponse getSummary() {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        List<Long> accessibleProjectIds = currentUser.getUser().getGlobalRole() == GlobalRole.ADMIN
                ? projectRepository.findAllProjectIds()
                : projectRepository.findAccessibleProjectIds(currentUser.getId());

        long totalProjects = accessibleProjectIds.size();

        if (accessibleProjectIds.isEmpty()) {
            return new DashboardSummaryResponse(0, 0, 0, 0, 0, 0, 0, List.of());
        }

        Specification<Ticket> accessibleSpec = Specification.where(
                TicketSpecifications.hasProjectIds(accessibleProjectIds)
        );

        long totalTickets = ticketRepository.count(accessibleSpec);
        long openTickets = ticketRepository.count(accessibleSpec.and(TicketSpecifications.hasStatus(TicketStatus.OPEN)));
        long inProgressTickets = ticketRepository.count(accessibleSpec.and(TicketSpecifications.hasStatus(TicketStatus.IN_PROGRESS)));
        long doneTickets = ticketRepository.count(accessibleSpec.and(TicketSpecifications.hasStatus(TicketStatus.DONE)));
        long criticalTickets = ticketRepository.count(accessibleSpec.and(TicketSpecifications.hasPriority(TicketPriority.CRITICAL)));
        long myTickets = ticketRepository.count(accessibleSpec.and(TicketSpecifications.hasAssigneeId(currentUser.getId())));

        List<TicketSummaryResponse> recentTickets = ticketRepository.findAll(
                        accessibleSpec,
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
                ).getContent().stream()
                .map(TicketSummaryResponse::from)
                .toList();

        return new DashboardSummaryResponse(
                totalProjects,
                totalTickets,
                openTickets,
                inProgressTickets,
                doneTickets,
                myTickets,
                criticalTickets,
                recentTickets
        );
    }
}
