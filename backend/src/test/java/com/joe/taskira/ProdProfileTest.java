package com.joe.taskira;

import com.joe.taskira.config.DevAdminBootstrap;
import com.joe.taskira.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "prod", inheritProfiles = false)
class ProdProfileTest extends PostgreSqlIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext applicationContext;

    private RestTestClient client() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void apiDocsAreNotExposed() {
        boolean success = client().get().uri("/v3/api-docs").exchange()
                .returnResult().getStatus().is2xxSuccessful();

        assertThat(success).isFalse();
    }

    @Test
    void swaggerUiIsNotExposed() {
        boolean success = client().get().uri("/swagger-ui.html").exchange()
                .returnResult().getStatus().is2xxSuccessful();

        assertThat(success).isFalse();
    }

    @Test
    void sessionCookieIsSecureUnderTheProdProfile() {
        String csrfToken = client().get().uri("/api/v1/auth/me").exchange()
                .returnResult().getResponseCookies().getFirst("XSRF-TOKEN").getValue();
        String email = "prod-secure-cookie-" + UUID.randomUUID() + "@taskira.test";
        ExchangeResult result = client().post().uri("/api/v1/auth/register")
                .cookie("XSRF-TOKEN", csrfToken)
                .header("X-XSRF-TOKEN", csrfToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body("""
                        {
                          "firstName": "Prod",
                          "lastName": "Cookie",
                          "email": "%s",
                          "password": "Taskira-Prod-Cookie-42!",
                          "confirmPassword": "Taskira-Prod-Cookie-42!"
                        }
                        """.formatted(email))
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value())
                .as("register status; body=%s", new String(result.getResponseBodyContent()))
                .isEqualTo(201);

        ResponseCookie sessionCookie = result.getResponseCookies().getFirst("TASKIRA_SESSION");
        assertThat(sessionCookie).isNotNull();
        assertThat(sessionCookie.isSecure()).isTrue();
    }

    @Test
    void theDevAdminBootstrapIsNotWiredUpUnderTheProdProfile() {
        assertThat(applicationContext.getBeanNamesForType(DevAdminBootstrap.class)).isEmpty();
    }
}
