package com.joe.taskira.exports.controller;

import com.joe.taskira.attachments.port.DocumentStorage;
import com.joe.taskira.common.exception.ConflictException;
import com.joe.taskira.common.exception.ResourceNotFoundException;
import com.joe.taskira.common.util.SecurityUtils;
import com.joe.taskira.common.web.ApiVersion;
import com.joe.taskira.exports.dto.BulkExportJobResponse;
import com.joe.taskira.security.model.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;

/**
 * Admin-only (see ADR-0022): the only export in this module with unbounded volume
 * (every ticket, every project), the reason it runs as an asynchronous Spring Batch job
 * instead of a synchronous request like ExportController's two endpoints.
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/exports/tickets/batch")
@RequiredArgsConstructor
public class BulkExportController {

    private final JobLauncher bulkExportJobLauncher;
    private final Job ticketsBulkExportJob;
    private final JobExplorer jobExplorer;
    private final DocumentStorage documentStorage;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BulkExportJobResponse launch() {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        JobParameters parameters = new JobParametersBuilder()
                // Identifying by design: without a varying parameter, Spring Batch
                // treats a relaunch as the same JobInstance and refuses to rerun it.
                .addLong("launchedAt", System.currentTimeMillis())
                .addLong("adminUserId", currentUser.getId())
                .addString("adminEmail", currentUser.getUser().getEmail())
                .toJobParameters();

        JobExecution execution;
        try {
            execution = bulkExportJobLauncher.run(ticketsBulkExportJob, parameters);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to launch the bulk export job", e);
        }

        return BulkExportJobResponse.from(execution);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{jobExecutionId}")
    public BulkExportJobResponse status(@PathVariable Long jobExecutionId) {
        return BulkExportJobResponse.from(findExecutionOrThrow(jobExecutionId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{jobExecutionId}/download")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable Long jobExecutionId) {
        JobExecution execution = findExecutionOrThrow(jobExecutionId);

        if (execution.getStatus() != BatchStatus.COMPLETED) {
            throw new ConflictException("Export is not ready yet: " + execution.getStatus());
        }

        String storageKey = execution.getExecutionContext().getString("storageKey");

        StreamingResponseBody body = outputStream -> {
            try (InputStream in = documentStorage.retrieve(storageKey)) {
                in.transferTo(outputStream);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to stream the bulk export workbook", e);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"tickets-export-" + jobExecutionId + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    private JobExecution findExecutionOrThrow(Long jobExecutionId) {
        JobExecution execution = jobExplorer.getJobExecution(jobExecutionId);
        if (execution == null) {
            throw new ResourceNotFoundException("Export job not found");
        }
        return execution;
    }
}
