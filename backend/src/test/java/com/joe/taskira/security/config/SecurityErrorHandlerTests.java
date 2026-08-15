package com.joe.taskira.security.config;

import com.joe.taskira.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorHandlerTests {

    private static final String USERS_URL = "/api/v1/users";

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    void authenticationEntryPointReturnsAProblemDetailBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint(objectMapper).commence(
                request,
                response,
                new InsufficientAuthenticationException("Authentication required")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(body.path("status").asInt()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(body.path("detail").asText()).isEqualTo("Unauthorized");
        assertThat(body.path("instance").asText()).isEqualTo("/api/v1/projects");
    }

    @Test
    void accessDeniedHandlerReturnsAProblemDetailBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", USERS_URL);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAccessDeniedHandler(objectMapper).handle(
                request,
                response,
                new AccessDeniedException("Access denied")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(body.path("status").asInt()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(body.path("detail").asText()).isEqualTo("Access denied");
        assertThat(body.path("instance").asText()).isEqualTo(USERS_URL);
    }

    @Test
    void methodSecurityAccessDeniedIsMappedToForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", USERS_URL);
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<?> response = handler.handleAccessDeniedException(
                new AccessDeniedException("Access denied"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
