package com.joe.taskira.attachments.entity;

import com.joe.taskira.common.audit.AuditableEntity;
import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "attachments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attachments_storage_key", columnNames = "storage_key")
        }
)
public class Attachment extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    // Tika-detected, never the client-declared Content-Type header.
    @Column(name = "content_type", nullable = false, length = 127)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    // Server-generated UUID, never derived from originalFilename - the actual key
    // DocumentStorage uses to locate the file, so a filename can never become a path.
    @Column(name = "storage_key", nullable = false, length = 64)
    private String storageKey;
}
