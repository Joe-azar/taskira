package com.joe.taskira.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(

        @NotBlank(message = "Comment content is required")
        @Size(max = 4000, message = "Comment content must not exceed 4000 characters")
        String content
) {
}