package com.joe.taskira.exports;

import com.joe.taskira.attachments.port.DocumentStorage;
import com.joe.taskira.support.PostgreSqlIntegrationTest;
import com.joe.taskira.user.repository.UserRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real job mechanics, launched through the app's actual asynchronous JobLauncher (see
 * ADR-0022/BulkExportJobConfig) rather than a synchronous test-only substitute - proves
 * ticketsBulkExportJob really transitions to COMPLETED and produces a downloadable
 * workbook, not just that its Spring beans wire up.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BulkExportJobIT extends PostgreSqlIntegrationTest {

    private static final String SESSION_COOKIE = "TASKIRA_SESSION";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobLauncher bulkExportJobLauncher;

    @Autowired
    private Job ticketsBulkExportJob;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private DocumentStorage documentStorage;

    private RestTestClient client() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private record Actor(Long userId, String email, String sessionCookie, String csrfCookie) {
    }

    private Actor register(String label) {
        ExchangeResult csrfSeed = client().get().uri("/api/v1/auth/me").exchange().returnResult();
        String csrfToken = csrfSeed.getResponseCookies().getFirst(CSRF_COOKIE).getValue();
        String email = label + "-" + UUID.randomUUID() + "@taskira.test";

        ExchangeResult result = client().post().uri("/api/v1/auth/register")
                .cookie(CSRF_COOKIE, csrfToken)
                .header(CSRF_HEADER, csrfToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body("""
                        {
                          "firstName": "Bulk",
                          "lastName": "Export",
                          "email": "%s",
                          "password": "Taskira-Bulk-IT-42!",
                          "confirmPassword": "Taskira-Bulk-IT-42!"
                        }
                        """.formatted(email))
                .exchange()
                .returnResult();
        assertThat(result.getStatus().value()).isEqualTo(201);

        Long userId = userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
        String sessionCookie = result.getResponseCookies().getFirst(SESSION_COOKIE).getValue();
        return new Actor(userId, email, sessionCookie, csrfToken);
    }

    private long createProjectWithTickets(Actor owner, String code, int ticketCount) {
        ExchangeResult projectResult = client().post().uri("/api/v1/projects")
                .cookie(SESSION_COOKIE, owner.sessionCookie())
                .cookie(CSRF_COOKIE, owner.csrfCookie())
                .header(CSRF_HEADER, owner.csrfCookie())
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body("""
                        {"code": "%s", "name": "Bulk export project", "description": ""}
                        """.formatted(code))
                .exchange()
                .returnResult();
        assertThat(projectResult.getStatus().value()).isEqualTo(201);
        long projectId = JSON.readTree(new String(projectResult.getResponseBodyContent())).get("id").asLong();

        for (int i = 0; i < ticketCount; i++) {
            ExchangeResult ticketResult = client().post().uri("/api/v1/tickets")
                    .cookie(SESSION_COOKIE, owner.sessionCookie())
                    .cookie(CSRF_COOKIE, owner.csrfCookie())
                    .header(CSRF_HEADER, owner.csrfCookie())
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body("""
                            {"projectId": %d, "title": "Bulk ticket %d", "type": "TASK"}
                            """.formatted(projectId, i))
                    .exchange()
                    .returnResult();
            assertThat(ticketResult.getStatus().value()).isEqualTo(201);
        }

        return projectId;
    }

    @Test
    void jobCompletesAndProducesADownloadableWorkbookCoveringTheSeededProject() throws Exception {
        // This test launches the job directly through the beans, bypassing
        // BulkExportController's @PreAuthorize("hasRole('ADMIN')") entirely - see
        // BulkExportWiringIT for that access-control proof. adminUserId/adminEmail
        // below only need to be a real user for AuditService.record(...) to attribute
        // the resulting EXPORT_GENERATED event to; they carry no authorization meaning
        // at this layer.
        Actor actor = register("bulk-export-actor");

        String projectCode = "BLK" + actor.userId();
        createProjectWithTickets(actor, projectCode, 3);

        JobParameters parameters = new JobParametersBuilder()
                .addLong("launchedAt", (long) UUID.randomUUID().hashCode())
                .addLong("adminUserId", actor.userId())
                .addString("adminEmail", actor.email())
                .toJobParameters();

        JobExecution execution = bulkExportJobLauncher.run(ticketsBulkExportJob, parameters);

        JobExecution finished = waitForCompletion(execution.getId());

        assertThat(finished.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        String storageKey = finished.getExecutionContext().getString("storageKey");
        assertThat(storageKey).isNotBlank();

        try (InputStream stored = documentStorage.retrieve(storageKey)) {
            byte[] bytes = stored.readAllBytes();
            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                Sheet sheet = workbook.getSheet(projectCode);
                assertThat(sheet).as("sheet for project " + projectCode).isNotNull();

                Row header = sheet.getRow(0);
                assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Reference");
                // Header + 3 seeded tickets, exactly the ones created above.
                assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(4);
            }
        }
    }

    private JobExecution waitForCompletion(long jobExecutionId) throws InterruptedException {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(30).toNanos();
        JobExecution execution = jobExplorer.getJobExecution(jobExecutionId);
        while (execution != null && execution.getStatus().isRunning() && System.nanoTime() < deadline) {
            Thread.sleep(200);
            execution = jobExplorer.getJobExecution(jobExecutionId);
        }
        assertThat(execution).as("job execution never appeared").isNotNull();
        return execution;
    }
}
