package com.joe.taskira.exports;

import com.joe.taskira.audit.entity.AuditEvent;
import com.joe.taskira.audit.enums.AuditAction;
import com.joe.taskira.audit.repository.AuditEventRepository;
import com.joe.taskira.support.PostgreSqlIntegrationTest;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The full HTTP-triggered async flow (see ADR-0022): launch -> poll -> download, plus
 * the ADMIN-only access control BulkExportJobIT deliberately bypasses by calling the
 * batch beans directly. loginAsPersistedAdmin mirrors AuditWiringIT: a user must be
 * ADMIN in the database *before* logging in, because Spring Security snapshots the
 * role into the session at login time - promoting an already-logged-in user's role in
 * the database would not be reflected until they log in again.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BulkExportWiringIT extends PostgreSqlIntegrationTest {

    private static final String SESSION_COOKIE = "TASKIRA_SESSION";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String TEST_PASSWORD = "Taskira-BulkWiring-IT-42!";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private RestTestClient client() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private record Actor(Long userId, String email, String sessionCookie, String csrfCookie) {
    }

    private String seedCsrfToken() {
        ExchangeResult result = client().get().uri("/api/v1/auth/me").exchange().returnResult();
        return result.getResponseCookies().getFirst(CSRF_COOKIE).getValue();
    }

    private Actor register(String label) {
        String csrfToken = seedCsrfToken();
        String email = label + "-" + UUID.randomUUID() + "@taskira.test";

        ExchangeResult result = client().post().uri("/api/v1/auth/register")
                .cookie(CSRF_COOKIE, csrfToken)
                .header(CSRF_HEADER, csrfToken)
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body("""
                        {
                          "firstName": "Bulk",
                          "lastName": "Wiring",
                          "email": "%s",
                          "password": "%s",
                          "confirmPassword": "%s"
                        }
                        """.formatted(email, TEST_PASSWORD, TEST_PASSWORD))
                .exchange()
                .returnResult();
        assertThat(result.getStatus().value()).isEqualTo(201);

        Long userId = userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
        String sessionCookie = result.getResponseCookies().getFirst(SESSION_COOKIE).getValue();
        return new Actor(userId, email, sessionCookie, csrfToken);
    }

    private Actor loginAsPersistedAdmin(String email) {
        User admin = User.builder()
                .firstName("Bulk")
                .lastName("Admin")
                .email(email)
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .globalRole(GlobalRole.ADMIN)
                .active(true)
                .build();
        admin = userRepository.saveAndFlush(admin);

        String csrfToken = seedCsrfToken();
        ExchangeResult result = client().post().uri("/api/v1/auth/login")
                .cookie(CSRF_COOKIE, csrfToken)
                .header(CSRF_HEADER, csrfToken)
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, TEST_PASSWORD))
                .exchange()
                .returnResult();
        assertThat(result.getStatus().value()).isEqualTo(200);

        String sessionCookie = result.getResponseCookies().getFirst(SESSION_COOKIE).getValue();
        return new Actor(admin.getId(), email, sessionCookie, csrfToken);
    }

    private ExchangeResult authenticatedPost(Actor actor, String uri) {
        return client().post().uri(uri)
                .cookie(SESSION_COOKIE, actor.sessionCookie())
                .cookie(CSRF_COOKIE, actor.csrfCookie())
                .header(CSRF_HEADER, actor.csrfCookie())
                .exchange()
                .returnResult();
    }

    @Test
    void adminCanLaunchPollAndDownloadTheBulkExport() throws Exception {
        Actor admin = loginAsPersistedAdmin("bulk-wiring-admin-" + UUID.randomUUID() + "@taskira.test");

        ExchangeResult launchResult = authenticatedPost(admin, "/api/v1/exports/tickets/batch");
        assertThat(launchResult.getStatus().value()).isEqualTo(202);

        JsonNode launchBody = JSON.readTree(new String(launchResult.getResponseBodyContent()));
        long jobExecutionId = launchBody.get("jobExecutionId").asLong();

        String finalStatus = pollUntilTerminal(admin, jobExecutionId);
        assertThat(finalStatus).isEqualTo("COMPLETED");

        ExchangeResult downloadResult = client().get()
                .uri("/api/v1/exports/tickets/batch/" + jobExecutionId + "/download")
                .cookie(SESSION_COOKIE, admin.sessionCookie())
                .exchange()
                .returnResult();

        assertThat(downloadResult.getStatus().value()).isEqualTo(200);
        assertThat(downloadResult.getResponseHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment");

        // A real, readable workbook - not just non-empty bytes.
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(downloadResult.getResponseBodyContent()))) {
            assertThat(workbook.getNumberOfSheets()).isGreaterThanOrEqualTo(0);
        }

        List<AuditEvent> auditEvents = auditEventRepository.findAll();
        boolean auditedThisJob = auditEvents.stream().anyMatch(event ->
                event.getAction() == AuditAction.EXPORT_GENERATED
                        && admin.email().equals(event.getActorEmail())
                        && event.getEntityId() != null
                        && event.getEntityId() == jobExecutionId
        );
        assertThat(auditedThisJob).as("EXPORT_GENERATED audit event for this job execution").isTrue();
    }

    @Test
    void aNonAdminCannotLaunchTheBulkExport() {
        Actor user = register("bulk-wiring-user");

        ExchangeResult result = authenticatedPost(user, "/api/v1/exports/tickets/batch");

        assertThat(result.getStatus().value()).isEqualTo(403);
    }

    private String pollUntilTerminal(Actor admin, long jobExecutionId) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        String status = "STARTING";
        while (System.nanoTime() < deadline) {
            ExchangeResult statusResult = client().get()
                    .uri("/api/v1/exports/tickets/batch/" + jobExecutionId)
                    .cookie(SESSION_COOKIE, admin.sessionCookie())
                    .exchange()
                    .returnResult();
            assertThat(statusResult.getStatus().value()).isEqualTo(200);

            JsonNode body = JSON.readTree(new String(statusResult.getResponseBodyContent()));
            status = body.get("status").asText();
            if (!List.of("STARTING", "STARTED").contains(status)) {
                return status;
            }
            Thread.sleep(200);
        }
        return status;
    }
}
