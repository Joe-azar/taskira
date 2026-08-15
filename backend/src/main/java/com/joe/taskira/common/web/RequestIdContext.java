package com.joe.taskira.common.web;

public final class RequestIdContext {

    public static final String MDC_KEY = "requestId";

    public static final String RESPONSE_HEADER = "X-Request-Id";

    private RequestIdContext() {
    }
}
