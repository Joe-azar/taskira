package com.joe.taskira.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void optimisticLockingFailureIsMappedToConflict() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/tickets/1/status");
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ProblemDetail> response = handler.handleOptimisticLockingFailureException(
                new OptimisticLockingFailureException("Row was updated or deleted by another transaction"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getBody().getInstance()).hasToString("/api/v1/tickets/1/status");
    }
}
