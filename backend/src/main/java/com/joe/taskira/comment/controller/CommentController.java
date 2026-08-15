package com.joe.taskira.comment.controller;

import com.joe.taskira.comment.dto.CommentResponse;
import com.joe.taskira.comment.dto.CreateCommentRequest;
import com.joe.taskira.comment.dto.UpdateCommentRequest;
import com.joe.taskira.comment.service.CommentService;
import com.joe.taskira.common.web.ApiVersion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1)
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/tickets/{ticketId}/comments")
    public List<CommentResponse> listTicketComments(@PathVariable Long ticketId) {
        return commentService.listTicketComments(ticketId);
    }

    @PostMapping("/tickets/{ticketId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return commentService.createComment(ticketId, request);
    }

    @PutMapping("/comments/{commentId}")
    public CommentResponse updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return commentService.updateComment(commentId, request);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
    }
}