package com.joe.taskira.ticket.dto;

import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.enums.TicketPriority;
import com.joe.taskira.ticket.enums.TicketStatus;
import com.joe.taskira.ticket.enums.TicketType;

import java.time.Instant;

public record TicketSummaryResponse(
        Long id,
        String reference,
        String title,
        TicketType type,
        TicketStatus status,
        TicketPriority priority,
        String assigneeFullName,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketSummaryResponse from(Ticket ticket) {
        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getReference(),
                ticket.getTitle(),
                ticket.getType(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee() != null ? ticket.getAssignee().getFullName() : null,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}