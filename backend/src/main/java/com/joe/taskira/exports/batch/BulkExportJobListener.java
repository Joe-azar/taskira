package com.joe.taskira.exports.batch;

import com.joe.taskira.attachments.port.DocumentStorage;
import com.joe.taskira.audit.enums.AuditAction;
import com.joe.taskira.audit.enums.AuditEntityType;
import com.joe.taskira.audit.service.AuditService;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Finalizes the job-scoped workbook (see BulkExportJobConfig) in afterStep, not
 * JobExecutionListener.afterJob - confirmed against the Spring Batch 6.0.4 source that
 * AbstractJob.execute() sets and persists the job's terminal status (jobRepository.update)
 * BEFORE calling afterJob listeners, so a caller polling status alone could already see
 * COMPLETED while afterJob is still running (or hasn't started). afterStep runs as part
 * of the step's own completion, strictly before the job's overall status is determined,
 * so storageKey is guaranteed present by the time a caller can observe COMPLETED.
 */
@Component
public class BulkExportJobListener implements StepExecutionListener {

    private final Workbook bulkExportWorkbook;
    private final DocumentStorage documentStorage;
    private final AuditService auditService;
    private final JobRepository jobRepository;

    public BulkExportJobListener(
            Workbook bulkExportWorkbook,
            DocumentStorage documentStorage,
            AuditService auditService,
            JobRepository jobRepository
    ) {
        this.bulkExportWorkbook = bulkExportWorkbook;
        this.documentStorage = documentStorage;
        this.auditService = auditService;
        this.jobRepository = jobRepository;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
            storeAndAudit(stepExecution.getJobExecution());
        }
        return stepExecution.getExitStatus();
    }

    private void storeAndAudit(JobExecution jobExecution) {
        byte[] workbookBytes;
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            bulkExportWorkbook.write(buffer);
            workbookBytes = buffer.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize the bulk export workbook", e);
        } finally {
            if (bulkExportWorkbook instanceof SXSSFWorkbook streaming) {
                streaming.dispose();
            }
        }

        String storageKey;
        try (InputStream toStore = new ByteArrayInputStream(workbookBytes)) {
            storageKey = documentStorage.store(toStore);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store the bulk export workbook", e);
        }

        jobExecution.getExecutionContext().putString("storageKey", storageKey);
        // JobRepository.update(JobExecution) - the only persistence call
        // AbstractJob.execute() makes around job completion - never persists the
        // ExecutionContext (confirmed against the Spring Batch 6.0.4 source:
        // SimpleJobRepository.update(JobExecution) only calls jobExecutionDao). Without
        // this explicit call, storageKey would be mutated in memory but never reach
        // BATCH_JOB_EXECUTION_CONTEXT, so a fresh JobExplorer read (a separate HTTP
        // request, a different JVM under load) would never see it.
        jobRepository.updateExecutionContext(jobExecution);

        JobParameters parameters = jobExecution.getJobParameters();
        auditService.record(
                parameters.getLong("adminUserId"),
                parameters.getString("adminEmail"),
                AuditEntityType.EXPORT,
                jobExecution.getId(),
                AuditAction.EXPORT_GENERATED,
                "Bulk ticket export"
        );
    }
}
