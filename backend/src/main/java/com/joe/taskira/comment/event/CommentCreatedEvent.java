package com.joe.taskira.comment.event;

import java.util.Set;

/**
 * Published after a comment is created. recipientUserIds already excludes the
 * comment's own author (a ticket's creator and assignee commenting on their own
 * ticket shouldn't notify themselves).
 */
public record CommentCreatedEvent(
        Long commentId,
        Long ticketId,
        String ticketReference,
        String ticketTitle,
        Long authorId,
        Set<Long> recipientUserIds
) {
}
