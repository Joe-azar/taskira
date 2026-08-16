package com.joe.taskira.audit.dto;

import com.joe.taskira.audit.entity.AuditEvent;
import com.joe.taskira.audit.enums.AuditAction;
import com.joe.taskira.audit.enums.AuditEntityType;

import java.time.Instant;

public record AuditEventResponse(
        Long id,
        Instant occurredAt,
        Long actorId,
        String actorEmail,
        AuditEntityType entityType,
        Long entityId,
        AuditAction action,
        String detail,
        String requestId,
        String ipAddress
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getOccurredAt(),
                event.getActorId(),
                event.getActorEmail(),
                event.getEntityType(),
                event.getEntityId(),
                event.getAction(),
                event.getDetail(),
                event.getRequestId(),
                event.getIpAddress()
        );
    }
}
