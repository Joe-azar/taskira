package com.joe.taskira.ticket.entity;

import com.joe.taskira.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket_history")
public class TicketHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName;

    @Column(name = "old_value", length = 4000)
    private String oldValue;

    @Column(name = "new_value", length = 4000)
    private String newValue;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @PrePersist
    public void prePersist() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}