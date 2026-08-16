package com.joe.taskira.observability;

import com.joe.taskira.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Actuator runs on its own port (management.server.port), never published to the host -
 * see infra/docker-compose.yml. That separation is the real security boundary, not an
 * app-level credential, so this proves two things: the isolated port serves exactly the
 * allowlisted endpoints and nothing else, and the main application port serves none of it.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0"
)
class ActuatorSecurityIT extends PostgreSqlIntegrationTest {

    @LocalServerPort
    private int appPort;

    @LocalManagementPort
    private int managementPort;

    private RestTestClient appClient() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + appPort).build();
    }

    private RestTestClient managementClient() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + managementPort).build();
    }

    @Test
    void healthAndItsProbeGroupsAreReachableAnonymouslyOnTheManagementPort() {
        assertThat(managementClient().get().uri("/actuator/health").exchange().returnResult().getStatus().value())
                .isEqualTo(200);
        assertThat(managementClient().get().uri("/actuator/health/readiness").exchange().returnResult().getStatus().value())
                .isEqualTo(200);
        assertThat(managementClient().get().uri("/actuator/health/liveness").exchange().returnResult().getStatus().value())
                .isEqualTo(200);
        assertThat(managementClient().get().uri("/actuator/info").exchange().returnResult().getStatus().value())
                .isEqualTo(200);
    }

    @Test
    void prometheusEndpointReturnsScrapableTextOnTheManagementPort() {
        ExchangeResult result = managementClient().get().uri("/actuator/prometheus").exchange().returnResult();

        assertThat(result.getStatus().value()).isEqualTo(200);
        String body = new String(result.getResponseBodyContent());
        assertThat(body).contains("jvm_memory_used_bytes");
    }

    // EndpointRequest.toAnyEndpoint() (SecurityConfig) only matches endpoints that are
    // actually mapped - i.e. exposed via management.endpoints.web.exposure.include. An
    // excluded id like "env" has no WebMvcEndpointHandlerMapping at all, so the request
    // falls through to anyRequest().authenticated() and an anonymous caller gets 401, not
    // 404 - Security runs before Spring MVC ever gets a chance to say "no handler found".
    // The property under test - unreachable without authentication - holds either way.
    @Test
    void nonAllowlistedEndpointsAreNotExposedEvenOnTheManagementPort() {
        assertThat(managementClient().get().uri("/actuator/env").exchange().returnResult().getStatus().value())
                .isEqualTo(401);
        assertThat(managementClient().get().uri("/actuator/beans").exchange().returnResult().getStatus().value())
                .isEqualTo(401);
        assertThat(managementClient().get().uri("/actuator/configprops").exchange().returnResult().getStatus().value())
                .isEqualTo(401);
        assertThat(managementClient().get().uri("/actuator/mappings").exchange().returnResult().getStatus().value())
                .isEqualTo(401);
        assertThat(managementClient().get().uri("/actuator/heapdump").exchange().returnResult().getStatus().value())
                .isEqualTo(401);
    }

    // Same mechanism as above: with a separate management port, actuator endpoints are
    // only mapped in the management context, so EndpointRequest.toAnyEndpoint() doesn't
    // match this path on the main port either - anonymous falls through to 401, not 404.
    @Test
    void actuatorHealthIsNotUsableFromTheMainApplicationPort() {
        assertThat(appPort).isNotEqualTo(managementPort);
        assertThat(appClient().get().uri("/actuator/health").exchange().returnResult().getStatus().value())
                .isEqualTo(401);
    }
}
