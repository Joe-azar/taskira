package com.joe.taskira.security.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joe.taskira.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorHandlerTests {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void authenticationEntryPointReturnsJsonWithTimestamp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/projects");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint(objectMapper).commence(
                request,
                response,
                new InsufficientAuthenticationException("Authentication required")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(body.path("timestamp").asText()).isNotBlank();
        assertThat(body.path("status").asInt()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(body.path("path").asText()).isEqualTo("/api/projects");
    }

    @Test
    void accessDeniedHandlerReturnsJsonWithTimestamp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAccessDeniedHandler(objectMapper).handle(
                request,
                response,
                new AccessDeniedException("Access denied")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(body.path("timestamp").asText()).isNotBlank();
        assertThat(body.path("status").asInt()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(body.path("path").asText()).isEqualTo("/api/users");
    }

    @Test
    void methodSecurityAccessDeniedIsMappedToForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<?> response = handler.handleAccessDeniedException(
                new AccessDeniedException("Access denied"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
