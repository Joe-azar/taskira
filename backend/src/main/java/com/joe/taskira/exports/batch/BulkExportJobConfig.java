package com.joe.taskira.exports.batch;

import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.repository.TicketRepository;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * ticketsBulkExportJob: one chunk-oriented step, paginated over every ticket in the
 * database (see ADR-0022). Deliberately not @EnableBatchProcessing - Spring Boot's own
 * batch auto-configuration already provides JobRepository/JobExplorer/a default
 * (synchronous) JobLauncher; only the JobLauncher is overridden here, to run this job
 * asynchronously off the HTTP request thread.
 */
@Configuration
public class BulkExportJobConfig {

    public static final String JOB_NAME = "ticketsBulkExportJob";
    private static final int CHUNK_SIZE = 200;

    @Bean
    public ThreadPoolTaskExecutor bulkExportTaskExecutor() {
        // Bounded on purpose: this job is rare and admin-triggered, not a
        // high-throughput workload - two concurrent runs is already generous.
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setThreadNamePrefix("bulk-export-");
        // ThreadPoolTaskExecutor threads are non-daemon by default, and Spring's test-
        // context cache doesn't guarantee this bean's owning context gets closed (and
        // the pool shut down) before a Surefire/Failsafe fork tries to exit - a real,
        // contributing cause of the "Surefire is going to kill self fork JVM" message
        // this project's test forks log (not the only one; some residual delay remains
        // even with this set, not chased further - no test failure, ~30-60s overhead).
        // Daemon threads are the correct default regardless for a background worker
        // pool that should never be the reason the JVM stays alive.
        executor.setDaemon(true);
        executor.initialize();
        return executor;
    }

    @Bean
    public JobLauncher bulkExportJobLauncher(JobRepository jobRepository, ThreadPoolTaskExecutor bulkExportTaskExecutor)
            throws Exception {
        // The Spring Boot default JobLauncher runs synchronously (SyncTaskExecutor) -
        // launching ticketsBulkExportJob through it would block the HTTP request until
        // the whole export finished. This bean replaces it (Spring Boot's
        // auto-configured JobLauncher backs off once one is defined explicitly).
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(bulkExportTaskExecutor);
        launcher.afterPropertiesSet();
        return launcher;
    }

    // @JobScope, not a plain singleton: BulkExportJobListener.afterJob(...) reads this
    // same instance after the step has finished, and a singleton would be silently
    // shared (and corrupted) across two job executions running at once.
    @Bean
    @JobScope
    public Workbook bulkExportWorkbook() {
        return new SXSSFWorkbook();
    }

    @Bean
    @StepScope
    public ItemReader<Ticket> ticketPagingItemReader(TicketRepository ticketRepository) {
        return new TicketPagingItemReader(ticketRepository, CHUNK_SIZE);
    }

    // @JobScope like the workbook it wraps, for the same reason: its per-sheet row
    // counters must not leak between concurrent executions.
    @Bean
    @JobScope
    public ItemWriter<Ticket> bulkExportItemWriter(Workbook bulkExportWorkbook) {
        return new BulkExportItemWriter(bulkExportWorkbook);
    }

    @Bean
    public Step bulkExportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<Ticket> ticketPagingItemReader,
            ItemWriter<Ticket> bulkExportItemWriter,
            BulkExportJobListener bulkExportJobListener
    ) {
        // chunk(CHUNK_SIZE) alone defaults to a no-op ResourcelessTransactionManager
        // (verified against the Spring Batch 6.0.4 source) - the real JPA transaction
        // manager must be wired explicitly or chunk commits would never actually be
        // transactional against the datasource.
        //
        // bulkExportJobListener is registered here (StepExecutionListener), not as a
        // JobExecutionListener on the job below - see BulkExportJobListener's own
        // Javadoc for why afterJob runs too late for a caller polling job status alone.
        return new StepBuilder("bulkExportStep", jobRepository)
                .<Ticket, Ticket>chunk(CHUNK_SIZE)
                .transactionManager(transactionManager)
                .reader(ticketPagingItemReader)
                .writer(bulkExportItemWriter)
                .listener(bulkExportJobListener)
                .build();
    }

    @Bean
    public Job ticketsBulkExportJob(JobRepository jobRepository, Step bulkExportStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(bulkExportStep)
                .build();
    }
}
