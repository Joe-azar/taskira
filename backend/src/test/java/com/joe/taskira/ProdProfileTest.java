package com.joe.taskira;

import com.joe.taskira.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "prod", inheritProfiles = false)
class ProdProfileTest extends PostgreSqlIntegrationTest {

    @LocalServerPort
    private int port;

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
}
