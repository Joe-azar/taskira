package com.joe.taskira.ticket.controller;

import com.joe.taskira.common.web.ApiVersion;
import com.joe.taskira.ticket.dto.*;
import com.joe.taskira.ticket.enums.TicketPriority;
import com.joe.taskira.ticket.enums.TicketStatus;
import com.joe.taskira.ticket.enums.TicketType;
import com.joe.taskira.ticket.service.TicketHistoryService;
import com.joe.taskira.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1)
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketHistoryService ticketHistoryService;

    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse createTicket(@Valid @RequestBody CreateTicketRequest request) {
        return ticketService.createTicket(request);
    }

    @GetMapping("/projects/{projectId}/tickets")
    public List<TicketSummaryResponse> listProjectTickets(@PathVariable Long projectId) {
        return ticketService.listProjectTickets(projectId);
    }

    @GetMapping("/tickets")
    public List<TicketSummaryResponse> searchTickets(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketType type,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false, name = "q") String keyword
    ) {
        return ticketService.searchTickets(
                projectId,
                status,
                priority,
                type,
                creatorId,
                assigneeId,
                unassigned,
                keyword
        );
    }

    @GetMapping("/tickets/paged")
    public TicketPageResponse searchTicketsPaged(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketType type,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ticketService.searchTicketsPage(
                projectId,
                status,
                priority,
                type,
                creatorId,
                assigneeId,
                unassigned,
                keyword,
                page,
                size,
                sort
        );
    }

    @GetMapping("/tickets/{ticketId}")
    public TicketResponse getTicketById(@PathVariable Long ticketId) {
        return ticketService.getTicketById(ticketId);
    }

    @GetMapping("/tickets/{ticketId}/history")
    public List<TicketHistoryResponse> listTicketHistory(@PathVariable Long ticketId) {
        return ticketHistoryService.listTicketHistory(ticketId);
    }

    @PatchMapping("/tickets/{ticketId}/status")
    public TicketResponse updateStatus(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        return ticketService.updateStatus(ticketId, request);
    }

    @PatchMapping("/tickets/{ticketId}/assignee")
    public TicketResponse updateAssignee(
            @PathVariable Long ticketId,
            @RequestBody UpdateTicketAssigneeRequest request
    ) {
        return ticketService.updateAssignee(ticketId, request);
    }

    @PutMapping("/tickets/{ticketId}")
    public TicketResponse updateTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketRequest request
    ) {
        return ticketService.updateTicket(ticketId, request);
    }
}