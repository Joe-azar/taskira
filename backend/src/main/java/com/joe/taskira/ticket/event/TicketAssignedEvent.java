package com.joe.taskira.ticket.event;

/**
 * Published after a ticket's assignee actually changes to a new, non-null user.
 * Carries identifiers only - listeners resolve current email/active status
 * themselves (e.g. via UserRepository) rather than trusting a snapshot that could be
 * stale by the time the event is handled.
 */
public record TicketAssignedEvent(
        Long ticketId,
        String ticketReference,
        String ticketTitle,
        Long assigneeId,
        Long actorId
) {
}
