package com.joe.taskira.comment.service;

import com.joe.taskira.comment.dto.CommentResponse;
import com.joe.taskira.comment.dto.CreateCommentRequest;
import com.joe.taskira.comment.dto.UpdateCommentRequest;
import com.joe.taskira.comment.entity.Comment;
import com.joe.taskira.comment.repository.CommentRepository;
import com.joe.taskira.common.exception.ForbiddenException;
import com.joe.taskira.common.exception.ResourceNotFoundException;
import com.joe.taskira.common.util.SecurityUtils;
import com.joe.taskira.project.entity.Project;
import com.joe.taskira.project.entity.ProjectMember;
import com.joe.taskira.project.enums.ProjectRole;
import com.joe.taskira.project.repository.ProjectMemberRepository;
import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.repository.TicketRepository;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public CommentResponse createComment(Long ticketId, CreateCommentRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        assertCanAccessProject(ticket.getProject());

        Comment comment = Comment.builder()
                .ticket(ticket)
                .user(currentUser)
                .content(request.content().trim())
                .build();

        comment = commentRepository.save(comment);

        return CommentResponse.from(commentRepository.findByIdWithRelations(comment.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found after creation")));
    }

    @Transactional
    public List<CommentResponse> listTicketComments(Long ticketId) {
        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        assertCanAccessProject(ticket.getProject());

        return commentRepository.findByTicketIdWithRelations(ticketId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    public CommentResponse updateComment(Long commentId, UpdateCommentRequest request) {
        Comment comment = findCommentOrThrow(commentId);

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only edit your own comments");
        }

        comment.setContent(request.content().trim());
        return CommentResponse.from(comment);
    }

    public void deleteComment(Long commentId) {
        Comment comment = findCommentOrThrow(commentId);

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        boolean isAuthor = comment.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getUser().getGlobalRole() == GlobalRole.ADMIN;
        boolean isProjectOwner = comment.getTicket().getProject().getOwner().getId().equals(currentUser.getId());

        if (!isAuthor && !isAdmin && !isProjectOwner) {
            ProjectMember membership = projectMemberRepository
                    .findByProjectIdAndUserId(comment.getTicket().getProject().getId(), currentUser.getId())
                    .orElse(null);

            boolean canManage = membership != null
                    && (membership.getProjectRole() == ProjectRole.OWNER || membership.getProjectRole() == ProjectRole.MANAGER);

            if (!canManage) {
                throw new ForbiddenException("You are not allowed to delete this comment");
            }
        }

        commentRepository.delete(comment);
    }

    private Comment findCommentOrThrow(Long commentId) {
        return commentRepository.findByIdWithRelations(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    }

    private void assertCanAccessProject(Project project) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (currentUser.getUser().getGlobalRole() == GlobalRole.ADMIN) {
            return;
        }

        if (project.getOwner().getId().equals(currentUser.getId())) {
            return;
        }

        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), currentUser.getId());

        if (!isMember) {
            throw new ForbiddenException("You are not allowed to access this project");
        }
    }
}