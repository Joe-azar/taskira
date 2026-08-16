package com.joe.taskira.exports.dto;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;

import java.time.LocalDateTime;

public record BulkExportJobResponse(
        long jobExecutionId,
        String status,
        String exitCode,
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean downloadReady
) {
    public static BulkExportJobResponse from(JobExecution execution) {
        return new BulkExportJobResponse(
                execution.getId(),
                execution.getStatus().name(),
                execution.getExitStatus().getExitCode(),
                execution.getStartTime(),
                execution.getEndTime(),
                execution.getStatus() == BatchStatus.COMPLETED
        );
    }
}
