package com.joe.taskira.ticket.dto;

import com.joe.taskira.ticket.enums.TicketPriority;
import com.joe.taskira.ticket.enums.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTicketRequest(

        @NotNull(message = "Project id is required")
        Long projectId,

        @NotBlank(message = "Ticket title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @Size(max = 4000, message = "Description must not exceed 4000 characters")
        String description,

        @NotNull(message = "Ticket type is required")
        TicketType type,

        TicketPriority priority,

        Long assigneeId,

        LocalDate dueDate
) {
}