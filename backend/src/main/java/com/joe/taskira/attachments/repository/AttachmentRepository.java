package com.joe.taskira.attachments.repository;

import com.joe.taskira.attachments.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    @Query("""
            select a
            from Attachment a
            join fetch a.ticket t
            join fetch a.uploader u
            where t.id = :ticketId
            order by a.createdAt desc
            """)
    List<Attachment> findByTicketIdWithRelations(Long ticketId);

    @Query("""
            select a
            from Attachment a
            join fetch a.ticket t
            join fetch a.uploader u
            where a.id = :attachmentId
            """)
    java.util.Optional<Attachment> findByIdWithRelations(Long attachmentId);
}
