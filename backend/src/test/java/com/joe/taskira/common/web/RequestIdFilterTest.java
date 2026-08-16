package com.joe.taskira.common.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesARequestIdWhenTheClientSendsNone() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] requestIdSeenByDownstream = new String[1];

        filter.doFilter(request, response, (req, res) -> requestIdSeenByDownstream[0] = MDC.get(RequestIdContext.MDC_KEY));

        String header = response.getHeader(RequestIdContext.RESPONSE_HEADER);
        assertThat(header).isNotBlank();
        assertThat(requestIdSeenByDownstream[0]).isEqualTo(header);
    }

    @Test
    void echoesBackAWellFormedClientSuppliedRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader(RequestIdContext.REQUEST_HEADER, "client-supplied-id-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(RequestIdContext.RESPONSE_HEADER)).isEqualTo("client-supplied-id-123");
    }

    @Test
    void replacesAMalformedClientSuppliedRequestIdWithAFreshOne() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader(RequestIdContext.REQUEST_HEADER, "not valid\r\nInjected: header");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String header = response.getHeader(RequestIdContext.RESPONSE_HEADER);
        assertThat(header).isNotEqualTo("not valid\r\nInjected: header");
        assertThat(header).matches("^[a-zA-Z0-9-]{1,64}$");
    }

    @Test
    void clearsMdcAfterTheRequestEvenWhenDownstreamThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                filter.doFilter(request, response, (req, res) -> {
                    throw new RuntimeException("downstream failure");
                })
        ).isInstanceOf(RuntimeException.class);

        assertThat(MDC.get(RequestIdContext.MDC_KEY)).isNull();
    }
}
