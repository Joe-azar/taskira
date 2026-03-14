package com.joe.taskira.comment.repository;

import com.joe.taskira.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
            select c
            from Comment c
            join fetch c.ticket t
            join fetch c.user u
            where t.id = :ticketId
            order by c.createdAt asc
            """)
    List<Comment> findByTicketIdWithRelations(Long ticketId);

    @Query("""
            select c
            from Comment c
            join fetch c.ticket t
            join fetch c.user u
            where c.id = :commentId
            """)
    Optional<Comment> findByIdWithRelations(Long commentId);
}