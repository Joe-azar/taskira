package com.joe.taskira.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Accepts a client-supplied X-Request-Id if it looks safe (never blindly trusted - an
 * attacker-controlled value would otherwise reach every log line and MDC), otherwise
 * generates one. Every response, success or error, echoes it back so a report from a user
 * or a failing E2E run can be correlated to the exact backend log lines and audit_events row
 * for that request.
 */
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("^[a-zA-Z0-9-]{1,64}$");

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String incoming = request.getHeader(RequestIdContext.REQUEST_HEADER);
        String requestId = (incoming != null && SAFE_REQUEST_ID.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString();

        MDC.put(RequestIdContext.MDC_KEY, requestId);
        response.setHeader(RequestIdContext.RESPONSE_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestIdContext.MDC_KEY);
        }
    }
}
