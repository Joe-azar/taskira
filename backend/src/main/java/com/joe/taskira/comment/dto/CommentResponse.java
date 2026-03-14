package com.joe.taskira.comment.dto;

import com.joe.taskira.comment.entity.Comment;
import com.joe.taskira.user.dto.UserSummaryResponse;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long ticketId,
        UserSummaryResponse author,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                UserSummaryResponse.from(comment.getUser()),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}