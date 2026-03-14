package com.joe.taskira.ticket.dto;

import com.joe.taskira.ticket.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(

        @NotNull(message = "Status is required")
        TicketStatus status
) {
}