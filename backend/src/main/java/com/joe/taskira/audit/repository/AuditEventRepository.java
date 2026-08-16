package com.joe.taskira.audit.repository;

import com.joe.taskira.audit.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    // Secondary id tiebreaker: occurredAt is millisecond-resolution, so two events recorded
    // in the same request (or the same test) can legitimately tie on it.
    Page<AuditEvent> findAllByOrderByOccurredAtDescIdDesc(Pageable pageable);
}
