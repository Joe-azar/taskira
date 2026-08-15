package com.joe.taskira.config;

import com.joe.taskira.support.PostgreSqlIntegrationTest;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "dev", inheritProfiles = false)
class DevAdminBootstrapIT extends PostgreSqlIntegrationTest {

    private static final String DEFAULT_ADMIN_EMAIL = "admin@taskira.test";
    private static final String DEFAULT_ADMIN_PASSWORD = "Taskira-Admin-42!";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DevAdminBootstrap devAdminBootstrap;

    private RestTestClient client() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void createsExactlyOneActiveAdminOnStartup() {
        Optional<User> admin = userRepository.findByEmailIgnoreCase(DEFAULT_ADMIN_EMAIL);

        assertThat(admin).isPresent();
        assertThat(admin.get().getGlobalRole()).isEqualTo(GlobalRole.ADMIN);
        assertThat(admin.get().isActive()).isTrue();
    }

    @Test
    void runningTheBootstrapAgainDoesNotDuplicateOrResetTheAdmin() {
        User seeded = userRepository.findByEmailIgnoreCase(DEFAULT_ADMIN_EMAIL).orElseThrow();
        String originalHash = seeded.getPasswordHash();

        ApplicationArguments noArgs = new DefaultApplicationArguments();
        assertThatCode(() -> devAdminBootstrap.run(noArgs)).doesNotThrowAnyException();

        long adminCount = userRepository.findAllByOrderByFirstNameAscLastNameAsc().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(DEFAULT_ADMIN_EMAIL))
                .count();
        assertThat(adminCount).isEqualTo(1);
        assertThat(userRepository.findByEmailIgnoreCase(DEFAULT_ADMIN_EMAIL).orElseThrow().getPasswordHash())
                .isEqualTo(originalHash);
    }

    @Test
    void theDefaultAdminCanLogInAndIsRecognisedAsAdmin() {
        String csrfToken = client().get().uri("/api/v1/auth/me").exchange()
                .returnResult().getResponseCookies().getFirst("XSRF-TOKEN").getValue();

        ExchangeResult result = client().post().uri("/api/v1/auth/login")
                .cookie("XSRF-TOKEN", csrfToken)
                .header("X-XSRF-TOKEN", csrfToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD))
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value())
                .as("login status; body=%s", new String(result.getResponseBodyContent()))
                .isEqualTo(200);
        assertThat(new String(result.getResponseBodyContent())).contains("\"globalRole\":\"ADMIN\"");
    }
}
