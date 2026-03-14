package com.joe.taskira.dashboard.dto;

import com.joe.taskira.ticket.dto.TicketSummaryResponse;

import java.util.List;

public record DashboardSummaryResponse(
        long totalProjects,
        long totalTickets,
        long openTickets,
        long inProgressTickets,
        long doneTickets,
        long myTickets,
        long criticalTickets,
        List<TicketSummaryResponse> recentTickets
) {
}
