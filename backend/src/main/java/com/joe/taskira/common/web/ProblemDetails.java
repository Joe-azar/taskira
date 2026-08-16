package com.joe.taskira.common.web;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

/**
 * The one place a ProblemDetail body is built, shared by GlobalExceptionHandler (controller
 * layer) and RestAuthenticationEntryPoint/RestAccessDeniedHandler (security filter layer,
 * which never reaches GlobalExceptionHandler since it runs before the DispatcherServlet).
 * Every error response carries the current request's correlation id as an extension
 * property, so it matches the X-Request-Id response header and the corresponding log lines.
 */
public final class ProblemDetails {

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatus status, String detail, String path) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setInstance(URI.create(path));

        String requestId = MDC.get(RequestIdContext.MDC_KEY);
        if (requestId != null) {
            body.setProperty("requestId", requestId);
        }

        return body;
    }
}
