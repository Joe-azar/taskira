package com.joe.taskira.audit.service;

import com.joe.taskira.audit.entity.AuditEvent;
import com.joe.taskira.audit.enums.AuditAction;
import com.joe.taskira.audit.enums.AuditEntityType;
import com.joe.taskira.audit.repository.AuditEventRepository;
import com.joe.taskira.common.web.RequestIdContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * The actor is always an explicit parameter, never resolved internally from
 * SecurityContextHolder - mirrors TicketHistoryService.logChange(Ticket, User changedBy, ...),
 * and is the only way to record actor-less events correctly (a failed login has no
 * authenticated principal to read; a logout handler runs after the security context may
 * already be cleared).
 */
@org.springframework.modulith.NamedInterface
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    // REQUIRES_NEW, not the caller's transaction: several call sites (a failed login being
    // the clearest) record an event describing why the enclosing transaction is about to
    // roll back. Joining that transaction would roll the audit row back right along with it,
    // silently losing exactly the events most worth keeping.
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void record(
            Long actorId,
            String actorEmail,
            AuditEntityType entityType,
            Long entityId,
            AuditAction action,
            String detail
    ) {
        AuditEvent event = AuditEvent.builder()
                .actorId(actorId)
                .actorEmail(actorEmail)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .detail(detail)
                .requestId(MDC.get(RequestIdContext.MDC_KEY))
                .ipAddress(currentRemoteAddress())
                .build();

        auditEventRepository.save(event);
    }

    private String currentRemoteAddress() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getRemoteAddr();
        }
        return null;
    }
}
