package com.joe.taskira.security;

import com.joe.taskira.audit.entity.AuditEvent;
import com.joe.taskira.audit.enums.AuditAction;
import com.joe.taskira.audit.repository.AuditEventRepository;
import com.joe.taskira.support.PostgreSqlIntegrationTest;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionAuthenticationIT extends PostgreSqlIntegrationTest {

    private static final String SESSION_COOKIE = "TASKIRA_SESSION";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String TEST_PASSWORD = "Taskira-Session-IT-42!";
    private static final String AUTH_ME_URL = "/api/v1/auth/me";
    private static final String CONTENT_TYPE_JSON = "application/json";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private RestTestClient client() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private record Registered(String email, String sessionCookie, String csrfCookie) {
    }

    /**
     * Mirrors what the real Angular app does: an anonymous GET (bootstrapSession's call to
     * /auth/me) seeds the XSRF-TOKEN cookie before any form can be submitted. Registration
     * and login are themselves POSTs and are not CSRF-exempt (exempting them would reopen
     * login-CSRF), so every anonymous client - including this test - needs that token first.
     */
    private String seedCsrfToken() {
        ExchangeResult result = client().get().uri(AUTH_ME_URL).exchange().returnResult();
        return cookieValue(result, CSRF_COOKIE);
    }

    private Registered register(String label) {
        String csrfToken = seedCsrfToken();
        String email = label + "-" + UUID.randomUUID() + "@taskira.test";
        String body = """
                {
                  "firstName": "Session",
                  "lastName": "Test",
                  "email": "%s",
                  "password": "%s",
                  "confirmPassword": "%s"
                }
                """.formatted(email, TEST_PASSWORD, TEST_PASSWORD);

        ExchangeResult result = client().post().uri("/api/v1/auth/register")
                .cookie(CSRF_COOKIE, csrfToken)
                .header(CSRF_HEADER, csrfToken)
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body(body)
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value())
                .as("register status; body=%s", new String(result.getResponseBodyContent()))
                .isEqualTo(201);

        // The register response only re-issues a Set-Cookie for XSRF-TOKEN if the value
        // actually changes; since we echoed back the exact value seedCsrfToken() already
        // captured, it stays unchanged and the cookie header is legitimately omitted here.
        return new Registered(email, cookieValue(result, SESSION_COOKIE), csrfToken);
    }

    private String cookieValue(ExchangeResult result, String name) {
        MultiValueMap<String, ResponseCookie> cookies = result.getResponseCookies();
        ResponseCookie cookie = cookies.getFirst(name);
        assertThat(cookie).as("Set-Cookie for " + name).isNotNull();
        return cookie.getValue();
    }

    @Test
    void loginWithValidCredentialsEstablishesASessionCookie() {
        Registered registered = register("login-ok");
        String csrfToken = seedCsrfToken();

        ExchangeResult result = client().post().uri("/api/v1/auth/login")
                .cookie(CSRF_COOKIE, csrfToken)
                .header(CSRF_HEADER, csrfToken)
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(registered.email(), TEST_PASSWORD))
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value()).isEqualTo(200);
        assertThat(cookieValue(result, SESSION_COOKIE)).isNotBlank();
    }

    @Test
    void loginWithWrongPasswordReturns401() {
        Registered registered = register("login-bad-password");
        String csrfToken = seedCsrfToken();

        ExchangeResult result = client().post().uri("/api/v1/auth/login")
                .cookie(CSRF_COOKIE, csrfToken)
                .header(CSRF_HEADER, csrfToken)
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body("""
                        {"email": "%s", "password": "not-the-real-password"}
                        """.formatted(registered.email()))
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value()).isEqualTo(401);
    }

    @Test
    void loginWithADisabledUserReturns401() {
        Registered registered = register("login-disabled");
        User user = userRepository.findByEmailIgnoreCase(registered.email()).orElseThrow();
        user.setActive(false);
        userRepository.saveAndFlush(user);
        String csrfToken = seedCsrfToken();

        ExchangeResult result = client().post().uri("/api/v1/auth/login")
                .cookie(CSRF_COOKIE, csrfToken)
                .header(CSRF_HEADER, csrfToken)
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(registered.email(), TEST_PASSWORD))
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value()).isEqualTo(401);
    }

    @Test
    void anonymousAccessToAProtectedRouteReturns401() {
        ExchangeResult result = client().get().uri(AUTH_ME_URL)
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value()).isEqualTo(401);
    }

    @Test
    void everyResponseCarriesAnXRequestIdHeaderAndTheProblemDetailBodyMatchesItOnError() {
        ExchangeResult anonymous = client().get().uri(AUTH_ME_URL).exchange().returnResult();
        String headerRequestId = anonymous.getResponseHeaders().getFirst("X-Request-Id");
        assertThat(headerRequestId).isNotBlank();

        String body = new String(anonymous.getResponseBodyContent());
        assertThat(body).contains("\"requestId\":\"" + headerRequestId + "\"");

        ExchangeResult ok = client().get().uri("/v3/api-docs").exchange().returnResult();
        assertThat(ok.getResponseHeaders().getFirst("X-Request-Id")).isNotBlank();
    }

    @Test
    void aUserCallingAnAdminEndpointReturns403() {
        Registered registered = register("forbidden-admin");

        ExchangeResult result = client().post().uri("/api/v1/users")
                .cookie(SESSION_COOKIE, registered.sessionCookie())
                .cookie(CSRF_COOKIE, registered.csrfCookie())
                .header(CSRF_HEADER, registered.csrfCookie())
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body("""
                        {
                          "firstName": "New",
                          "lastName": "Admin",
                          "email": "should-not-be-created@taskira.test",
                          "password": "Some-Password-42!",
                          "globalRole": "ADMIN",
                          "active": true
                        }
                        """)
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value()).isEqualTo(403);
    }

    @Test
    void sessionCookieAloneAuthenticatesASubsequentRequest() {
        Registered registered = register("session-reuse");

        ExchangeResult result = client().get().uri(AUTH_ME_URL)
                .cookie(SESSION_COOKIE, registered.sessionCookie())
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value()).isEqualTo(200);
        assertThat(new String(result.getResponseBodyContent())).contains(registered.email());
    }

    @Test
    void meReturns200WithASessionAnd401WithoutOne() {
        Registered registered = register("me-with-without-session");

        ExchangeResult withSession = client().get().uri(AUTH_ME_URL)
                .cookie(SESSION_COOKIE, registered.sessionCookie())
                .exchange()
                .returnResult();
        assertThat(withSession.getStatus().value()).isEqualTo(200);

        ExchangeResult withoutSession = client().get().uri(AUTH_ME_URL)
                .exchange()
                .returnResult();
        assertThat(withoutSession.getStatus().value()).isEqualTo(401);
    }

    @Test
    void logoutInvalidatesTheSessionSoAProtectedRouteBecomesUnreachable() {
        Registered registered = register("logout");

        ExchangeResult beforeLogout = client().get().uri(AUTH_ME_URL)
                .cookie(SESSION_COOKIE, registered.sessionCookie())
                .exchange()
                .returnResult();
        assertThat(beforeLogout.getStatus().value()).isEqualTo(200);

        ExchangeResult logout = client().post().uri("/api/v1/auth/logout")
                .cookie(SESSION_COOKIE, registered.sessionCookie())
                .cookie(CSRF_COOKIE, registered.csrfCookie())
                .header(CSRF_HEADER, registered.csrfCookie())
                .exchange()
                .returnResult();
        assertThat(logout.getStatus().value()).isEqualTo(204);

        ExchangeResult afterLogout = client().get().uri(AUTH_ME_URL)
                .cookie(SESSION_COOKIE, registered.sessionCookie())
                .exchange()
                .returnResult();
        assertThat(afterLogout.getStatus().value()).isEqualTo(401);
    }

    @Test
    void mutatingRequestWithoutCsrfTokenIsRejected() {
        Registered registered = register("csrf-missing");

        ExchangeResult result = client().post().uri("/api/v1/projects")
                .cookie(SESSION_COOKIE, registered.sessionCookie())
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body("""
                        {"code": "NOCSRF", "name": "No CSRF project", "description": ""}
                        """)
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value()).isEqualTo(403);
    }

    @Test
    void mutatingRequestWithAValidCsrfTokenIsAccepted() {
        Registered registered = register("csrf-valid");

        ExchangeResult result = client().post().uri("/api/v1/projects")
                .cookie(SESSION_COOKIE, registered.sessionCookie())
                .cookie(CSRF_COOKIE, registered.csrfCookie())
                .header(CSRF_HEADER, registered.csrfCookie())
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body("""
                        {"code": "CSRFOK", "name": "Valid CSRF project", "description": ""}
                        """)
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value()).isEqualTo(201);
    }

    @Test
    void sessionCookieIsHttpOnlyAndSameSiteLax() {
        String csrfToken = seedCsrfToken();
        String email = "cookie-attrs-" + UUID.randomUUID() + "@taskira.test";

        // The session cookie is only (re-)issued by the response that actually creates the
        // session, i.e. this register call itself - not a later request reusing it.
        ExchangeResult result = client().post().uri("/api/v1/auth/register")
                .cookie(CSRF_COOKIE, csrfToken)
                .header(CSRF_HEADER, csrfToken)
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body("""
                        {
                          "firstName": "Cookie",
                          "lastName": "Attrs",
                          "email": "%s",
                          "password": "%s",
                          "confirmPassword": "%s"
                        }
                        """.formatted(email, TEST_PASSWORD, TEST_PASSWORD))
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value())
                .as("register status; body=%s", new String(result.getResponseBodyContent()))
                .isEqualTo(201);

        ResponseCookie sessionCookie = result.getResponseCookies().getFirst(SESSION_COOKIE);
        assertThat(sessionCookie).isNotNull();
        assertThat(sessionCookie.isHttpOnly()).isTrue();
        assertThat(sessionCookie.getSameSite()).isEqualToIgnoringCase("Lax");
    }

    @Test
    void csrfCookieStaysJsReadableSoAngularCanEchoItBack() {
        ExchangeResult result = client().get().uri(AUTH_ME_URL).exchange().returnResult();

        ResponseCookie csrfCookie = result.getResponseCookies().getFirst(CSRF_COOKIE);
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.isHttpOnly()).isFalse();
    }

    @Test
    void successfulLoginWritesALoginSuccessAuditEventWithTheActorSet() {
        Registered registered = register("audit-login-success");
        String csrfToken = seedCsrfToken();

        ExchangeResult result = client().post().uri("/api/v1/auth/login")
                .cookie(CSRF_COOKIE, csrfToken)
                .header(CSRF_HEADER, csrfToken)
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(registered.email(), TEST_PASSWORD))
                .exchange()
                .returnResult();
        assertThat(result.getStatus().value()).isEqualTo(200);

        AuditEvent event = latestAuditEvent();
        assertThat(event.getAction()).isEqualTo(AuditAction.LOGIN_SUCCESS);
        assertThat(event.getActorId()).isNotNull();
        assertThat(event.getActorEmail()).isEqualTo(registered.email());
    }

    @Test
    void failedLoginWritesALoginFailureAuditEventWithNoActorAndNeverStoresThePassword() {
        Registered registered = register("audit-login-failure");
        String csrfToken = seedCsrfToken();

        ExchangeResult result = client().post().uri("/api/v1/auth/login")
                .cookie(CSRF_COOKIE, csrfToken)
                .header(CSRF_HEADER, csrfToken)
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body("""
                        {"email": "%s", "password": "not-the-real-password"}
                        """.formatted(registered.email()))
                .exchange()
                .returnResult();
        assertThat(result.getStatus().value()).isEqualTo(401);

        AuditEvent event = latestAuditEvent();
        assertThat(event.getAction()).isEqualTo(AuditAction.LOGIN_FAILURE);
        assertThat(event.getActorId()).isNull();
        assertThat(event.getActorEmail()).isEqualTo(registered.email());
        assertThat(event.getDetail()).isNull();
    }

    @Test
    void logoutWritesALogoutAuditEventForTheUserWhoWasLoggedIn() {
        Registered registered = register("audit-logout");

        ExchangeResult logout = client().post().uri("/api/v1/auth/logout")
                .cookie(SESSION_COOKIE, registered.sessionCookie())
                .cookie(CSRF_COOKIE, registered.csrfCookie())
                .header(CSRF_HEADER, registered.csrfCookie())
                .exchange()
                .returnResult();
        assertThat(logout.getStatus().value()).isEqualTo(204);

        AuditEvent event = latestAuditEvent();
        assertThat(event.getAction()).isEqualTo(AuditAction.LOGOUT);
        assertThat(event.getActorEmail()).isEqualTo(registered.email());
    }

    private AuditEvent latestAuditEvent() {
        List<AuditEvent> page = auditEventRepository
                .findAllByOrderByOccurredAtDescIdDesc(PageRequest.of(0, 1))
                .getContent();
        assertThat(page).as("at least one audit event recorded").isNotEmpty();
        return page.get(0);
    }
}
