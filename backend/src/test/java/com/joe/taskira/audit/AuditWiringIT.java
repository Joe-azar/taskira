package com.joe.taskira.audit;

import com.joe.taskira.audit.entity.AuditEvent;
import com.joe.taskira.audit.enums.AuditAction;
import com.joe.taskira.audit.repository.AuditEventRepository;
import com.joe.taskira.support.PostgreSqlIntegrationTest;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end proof that real mutations through the actual HTTP endpoints - not just
 * AuditService called directly - produce the audit_events rows described in the
 * SecurityConfig/AuthService wiring and the ticket/project/user service wiring.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditWiringIT extends PostgreSqlIntegrationTest {

    private static final String SESSION_COOKIE = "TASKIRA_SESSION";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String TEST_PASSWORD = "Taskira-Audit-IT-42!";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditEventRepository auditEventRepository;

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
                          "firstName": "Audit",
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

    private Actor loginAsPersistedAdmin(String email) {
        User admin = User.builder()
                .firstName("Audit")
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

    private long createProject(Actor owner, String code) {
        ExchangeResult result = authenticatedPost(owner, "/api/v1/projects", """
                {"code": "%s", "name": "Audit wiring project", "description": ""}
                """.formatted(code));
        assertThat(result.getStatus().value()).isEqualTo(201);
        return extractId(result);
    }

    private long createTicket(Actor owner, long projectId) {
        ExchangeResult result = authenticatedPost(owner, "/api/v1/tickets", """
                {"projectId": %d, "title": "Audit wiring ticket", "type": "TASK"}
                """.formatted(projectId));
        assertThat(result.getStatus().value()).isEqualTo(201);
        return extractId(result);
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    // The naive approach - a regex hunting for the first/last "id" in the body - breaks the
    // moment the response has a nested object with its own id (ProjectResponse.owner.id,
    // TicketResponse.creator.id, ...): a greedy match lands on the wrong one silently. Real
    // JSON parsing reads the top-level field only.
    private long extractId(ExchangeResult result) {
        String body = new String(result.getResponseBodyContent());
        return JSON.readTree(body).get("id").asLong();
    }

    private AuditEvent latestAuditEventFor(AuditAction action) {
        List<AuditEvent> recent = auditEventRepository
                .findAllByOrderByOccurredAtDescIdDesc(PageRequest.of(0, 20))
                .getContent();
        return recent.stream()
                .filter(event -> event.getAction() == action)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No audit_events row found for action " + action));
    }

    @Test
    void creatingATicketWritesATicketCreatedAuditEvent() {
        Actor owner = register("audit-ticket-create");
        long projectId = createProject(owner, "AWC" + owner.userId());
        long ticketId = createTicket(owner, projectId);

        AuditEvent event = latestAuditEventFor(AuditAction.TICKET_CREATED);
        assertThat(event.getEntityId()).isEqualTo(ticketId);
        assertThat(event.getActorId()).isEqualTo(owner.userId());
    }

    @Test
    void changingTicketStatusWritesATicketStatusChangedAuditEvent() {
        Actor owner = register("audit-ticket-status");
        long projectId = createProject(owner, "AWS" + owner.userId());
        long ticketId = createTicket(owner, projectId);

        ExchangeResult result = client().patch().uri("/api/v1/tickets/" + ticketId + "/status")
                .cookie(SESSION_COOKIE, owner.sessionCookie())
                .cookie(CSRF_COOKIE, owner.csrfCookie())
                .header(CSRF_HEADER, owner.csrfCookie())
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body("""
                        {"status": "IN_PROGRESS"}
                        """)
                .exchange()
                .returnResult();
        assertThat(result.getStatus().value()).isEqualTo(200);

        AuditEvent event = latestAuditEventFor(AuditAction.TICKET_STATUS_CHANGED);
        assertThat(event.getEntityId()).isEqualTo(ticketId);
        assertThat(event.getDetail()).isEqualTo("OPEN -> IN_PROGRESS");
    }

    @Test
    void creatingAProjectWritesAProjectCreatedAuditEvent() {
        Actor owner = register("audit-project-create");
        long projectId = createProject(owner, "AWP" + owner.userId());

        AuditEvent event = latestAuditEventFor(AuditAction.PROJECT_CREATED);
        assertThat(event.getEntityId()).isEqualTo(projectId);
        assertThat(event.getActorId()).isEqualTo(owner.userId());
    }

    @Test
    void archivingAProjectWritesAProjectArchivedAuditEvent() {
        Actor owner = register("audit-project-archive");
        long projectId = createProject(owner, "AWA" + owner.userId());

        ExchangeResult result = client().patch().uri("/api/v1/projects/" + projectId + "/archive")
                .cookie(SESSION_COOKIE, owner.sessionCookie())
                .cookie(CSRF_COOKIE, owner.csrfCookie())
                .header(CSRF_HEADER, owner.csrfCookie())
                .exchange()
                .returnResult();
        assertThat(result.getStatus().value()).isEqualTo(200);

        AuditEvent event = latestAuditEventFor(AuditAction.PROJECT_ARCHIVED);
        assertThat(event.getEntityId()).isEqualTo(projectId);
    }

    @Test
    void addingAndRemovingAProjectMemberWritesTheMatchingAuditEvents() {
        Actor owner = register("audit-member-owner");
        Actor member = register("audit-member-user");
        long projectId = createProject(owner, "AWM" + owner.userId());

        ExchangeResult addResult = authenticatedPost(owner, "/api/v1/projects/" + projectId + "/members", """
                {"userId": %d, "projectRole": "MEMBER"}
                """.formatted(member.userId()));
        assertThat(addResult.getStatus().value()).isEqualTo(201);

        AuditEvent addedEvent = latestAuditEventFor(AuditAction.PROJECT_MEMBER_ADDED);
        assertThat(addedEvent.getDetail()).contains(member.email());

        ExchangeResult removeResult = client().delete()
                .uri("/api/v1/projects/" + projectId + "/members/" + member.userId())
                .cookie(SESSION_COOKIE, owner.sessionCookie())
                .cookie(CSRF_COOKIE, owner.csrfCookie())
                .header(CSRF_HEADER, owner.csrfCookie())
                .exchange()
                .returnResult();
        assertThat(removeResult.getStatus().value()).isEqualTo(204);

        AuditEvent removedEvent = latestAuditEventFor(AuditAction.PROJECT_MEMBER_REMOVED);
        assertThat(removedEvent.getDetail()).contains(member.email());
    }

    @Test
    void changingAUsersGlobalRoleWritesAUserRoleChangedAuditEvent() {
        Actor admin = loginAsPersistedAdmin("audit-role-admin-" + UUID.randomUUID() + "@taskira.test");
        Actor target = register("audit-role-target");

        ExchangeResult result = client().put().uri("/api/v1/users/" + target.userId())
                .cookie(SESSION_COOKIE, admin.sessionCookie())
                .cookie(CSRF_COOKIE, admin.csrfCookie())
                .header(CSRF_HEADER, admin.csrfCookie())
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body("""
                        {
                          "firstName": "Audit",
                          "lastName": "Test",
                          "email": "%s",
                          "globalRole": "ADMIN",
                          "active": true
                        }
                        """.formatted(target.email()))
                .exchange()
                .returnResult();
        assertThat(result.getStatus().value()).isEqualTo(200);

        AuditEvent event = latestAuditEventFor(AuditAction.USER_ROLE_CHANGED);
        assertThat(event.getEntityId()).isEqualTo(target.userId());
        assertThat(event.getDetail()).isEqualTo("USER -> ADMIN");
    }

    @Test
    void deactivatingAUserWritesAUserDeactivatedAuditEvent() {
        Actor admin = loginAsPersistedAdmin("audit-status-admin-" + UUID.randomUUID() + "@taskira.test");
        Actor target = register("audit-status-target");

        ExchangeResult result = client().patch().uri("/api/v1/users/" + target.userId() + "/status")
                .cookie(SESSION_COOKIE, admin.sessionCookie())
                .cookie(CSRF_COOKIE, admin.csrfCookie())
                .header(CSRF_HEADER, admin.csrfCookie())
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body("""
                        {"active": false}
                        """)
                .exchange()
                .returnResult();
        assertThat(result.getStatus().value()).isEqualTo(200);

        AuditEvent event = latestAuditEventFor(AuditAction.USER_DEACTIVATED);
        assertThat(event.getEntityId()).isEqualTo(target.userId());
    }

    @Test
    void listingAuditEventsSucceedsForAnAdminAndIsForbiddenForAnOrdinaryUser() {
        Actor admin = loginAsPersistedAdmin("audit-list-admin-" + UUID.randomUUID() + "@taskira.test");
        Actor user = register("audit-list-user");
        // Guarantees at least one row exists to page over, independent of test order.
        createProject(admin, "AWL" + admin.userId());

        ExchangeResult asAdmin = client().get().uri("/api/v1/audit/events?page=0&size=5")
                .cookie(SESSION_COOKIE, admin.sessionCookie())
                .exchange()
                .returnResult();
        assertThat(asAdmin.getStatus().value()).isEqualTo(200);
        String body = new String(asAdmin.getResponseBodyContent());
        assertThat(body).contains("\"content\"");

        ExchangeResult asUser = client().get().uri("/api/v1/audit/events")
                .cookie(SESSION_COOKIE, user.sessionCookie())
                .exchange()
                .returnResult();
        assertThat(asUser.getStatus().value()).isEqualTo(403);
    }
}
