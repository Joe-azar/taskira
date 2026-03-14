package com.joe.taskira.ticket.dto;

import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.enums.TicketPriority;
import com.joe.taskira.ticket.enums.TicketStatus;
import com.joe.taskira.ticket.enums.TicketType;
import com.joe.taskira.user.dto.UserSummaryResponse;

import java.time.Instant;
import java.time.LocalDate;

public record TicketResponse(
        Long id,
        String reference,
        Long projectId,
        String projectCode,
        String projectName,
        String title,
        String description,
        TicketType type,
        TicketStatus status,
        TicketPriority priority,
        UserSummaryResponse creator,
        UserSummaryResponse assignee,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getReference(),
                ticket.getProject().getId(),
                ticket.getProject().getCode(),
                ticket.getProject().getName(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getType(),
                ticket.getStatus(),
                ticket.getPriority(),
                UserSummaryResponse.from(ticket.getCreator()),
                ticket.getAssignee() != null ? UserSummaryResponse.from(ticket.getAssignee()) : null,
                ticket.getDueDate(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}