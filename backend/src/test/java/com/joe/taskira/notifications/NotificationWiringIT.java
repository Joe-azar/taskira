package com.joe.taskira.notifications;

import com.joe.taskira.support.PostgreSqlIntegrationTest;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end proof that real mutations through the actual HTTP endpoints - ticket
 * assignment, a new comment - result in a real email actually received by a real SMTP
 * server, not just that NotificationService's send() call was invoked (see
 * NotificationServiceTest for that narrower unit-level check). Mirrors
 * AuditWiringIT's "prove through the real HTTP endpoints" discipline.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationWiringIT extends PostgreSqlIntegrationTest {

    private static final String SESSION_COOKIE = "TASKIRA_SESSION";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String TEST_PASSWORD = "Taskira-Notif-IT-42!";

    private static final GenericContainer<?> MAILPIT = new GenericContainer<>(
            DockerImageName.parse("axllent/mailpit:v1.30.7@sha256:d5ecbb067db3705fa953d79e1b7f81ef84038df67aba6c52825d8c02a1ea748a")
    ).withExposedPorts(1025, 8025);

    static {
        MAILPIT.start();
    }

    @DynamicPropertySource
    static void configureMail(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", MAILPIT::getHost);
        registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    }

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private RestTestClient client() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private RestTestClient mailpitClient() {
        return RestTestClient.bindToServer()
                .baseUrl("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025))
                .build();
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
                          "firstName": "Notif",
                          "lastName": "Test",
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

    private ExchangeResult authenticatedPost(Actor actor, String uri, String body) {
        return client().post().uri(uri)
                .cookie(SESSION_COOKIE, actor.sessionCookie())
                .cookie(CSRF_COOKIE, actor.csrfCookie())
                .header(CSRF_HEADER, actor.csrfCookie())
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body(body)
                .exchange()
                .returnResult();
    }

    private ExchangeResult authenticatedPatch(Actor actor, String uri, String body) {
        return client().patch().uri(uri)
                .cookie(SESSION_COOKIE, actor.sessionCookie())
                .cookie(CSRF_COOKIE, actor.csrfCookie())
                .header(CSRF_HEADER, actor.csrfCookie())
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body(body)
                .exchange()
                .returnResult();
    }

    private long extractId(ExchangeResult result) {
        String body = new String(result.getResponseBodyContent());
        return JSON.readTree(body).get("id").asLong();
    }

    private long createProject(Actor owner, String code) {
        ExchangeResult result = authenticatedPost(owner, "/api/v1/projects", """
                {"code": "%s", "name": "Notification wiring project", "description": ""}
                """.formatted(code));
        assertThat(result.getStatus().value()).isEqualTo(201);
        return extractId(result);
    }

    private void addMember(Actor owner, long projectId, long userId) {
        ExchangeResult result = authenticatedPost(owner, "/api/v1/projects/" + projectId + "/members", """
                {"userId": %d, "projectRole": "MEMBER"}
                """.formatted(userId));
        assertThat(result.getStatus().value()).isEqualTo(201);
    }

    private long createTicket(Actor owner, long projectId) {
        ExchangeResult result = authenticatedPost(owner, "/api/v1/tickets", """
                {"projectId": %d, "title": "Notification wiring ticket", "type": "TASK"}
                """.formatted(projectId));
        assertThat(result.getStatus().value()).isEqualTo(201);
        return extractId(result);
    }

    private JsonNode findMessageTo(String recipientEmail) {
        String body = new String(mailpitClient().get().uri("/api/v1/messages")
                .exchange().returnResult().getResponseBodyContent());
        JsonNode messages = JSON.readTree(body).get("messages");
        for (JsonNode message : messages) {
            for (JsonNode to : message.get("To")) {
                if (recipientEmail.equalsIgnoreCase(to.get("Address").asText())) {
                    return message;
                }
            }
        }
        return null;
    }

    // @TransactionalEventListener(AFTER_COMMIT) runs synchronously right as the
    // triggering transaction commits, before the HTTP response is written - so the
    // email is normally already in Mailpit by the time the request above returns. This
    // still polls briefly rather than asserting immediately, to absorb the real SMTP
    // round-trip and Mailpit's own indexing rather than assuming zero latency.
    private JsonNode waitForMessageTo(String recipientEmail) {
        return pollUntilNotNull(() -> findMessageTo(recipientEmail));
    }

    private <T> T pollUntilNotNull(Supplier<T> attempt) {
        long deadline = System.currentTimeMillis() + 10_000;
        T result = null;
        while (result == null && System.currentTimeMillis() < deadline) {
            result = attempt.get();
            if (result == null) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return result;
    }

    @Test
    void assigningATicketSendsARealEmailToTheAssigneeViaMailpit() {
        Actor owner = register("notif-assign-owner");
        Actor assignee = register("notif-assign-assignee");
        long projectId = createProject(owner, "NWA" + owner.userId());
        addMember(owner, projectId, assignee.userId());
        long ticketId = createTicket(owner, projectId);

        ExchangeResult result = authenticatedPatch(owner, "/api/v1/tickets/" + ticketId + "/assignee", """
                {"assigneeId": %d}
                """.formatted(assignee.userId()));
        assertThat(result.getStatus().value()).isEqualTo(200);

        JsonNode message = waitForMessageTo(assignee.email());
        assertThat(message).as("email to assignee %s", assignee.email()).isNotNull();
        assertThat(message.get("Subject").asText()).contains("Ticket assigned");
    }

    @Test
    void commentingOnATicketNotifiesTheCreatorButNotTheCommentAuthorThemselves() {
        Actor owner = register("notif-comment-owner");
        Actor commenter = register("notif-comment-author");
        long projectId = createProject(owner, "NWC" + owner.userId());
        addMember(owner, projectId, commenter.userId());
        long ticketId = createTicket(owner, projectId);

        ExchangeResult result = authenticatedPost(commenter, "/api/v1/tickets/" + ticketId + "/comments", """
                {"content": "A real comment for the notification wiring test"}
                """);
        assertThat(result.getStatus().value()).isEqualTo(201);

        JsonNode message = waitForMessageTo(owner.email());
        assertThat(message).as("email to ticket creator %s", owner.email()).isNotNull();
        assertThat(message.get("Subject").asText()).contains("New comment");

        assertThat(findMessageTo(commenter.email()))
                .as("comment author should not notify themselves")
                .isNull();
    }
}
