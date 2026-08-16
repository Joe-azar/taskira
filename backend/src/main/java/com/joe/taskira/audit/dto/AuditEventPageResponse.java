package com.joe.taskira.audit.dto;

import java.util.List;

public record AuditEventPageResponse(
        List<AuditEventResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
