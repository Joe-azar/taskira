package com.joe.taskira.ticket.dto;

import com.joe.taskira.ticket.entity.TicketHistory;
import com.joe.taskira.user.dto.UserSummaryResponse;

import java.time.Instant;

public record TicketHistoryResponse(
        Long id,
        String fieldName,
        String oldValue,
        String newValue,
        UserSummaryResponse changedBy,
        Instant changedAt
) {
    public static TicketHistoryResponse from(TicketHistory history) {
        return new TicketHistoryResponse(
                history.getId(),
                history.getFieldName(),
                history.getOldValue(),
                history.getNewValue(),
                UserSummaryResponse.from(history.getChangedBy()),
                history.getChangedAt()
        );
    }
}