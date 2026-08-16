package com.joe.taskira.audit.service;

import com.joe.taskira.audit.dto.AuditEventPageResponse;
import com.joe.taskira.audit.entity.AuditEvent;
import com.joe.taskira.audit.enums.AuditAction;
import com.joe.taskira.audit.enums.AuditEntityType;
import com.joe.taskira.audit.repository.AuditEventRepository;
import com.joe.taskira.common.web.RequestIdContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditService auditService;

    @AfterEach
    void clearContext() {
        MDC.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordPersistsEveryFieldFromTheExplicitActorAndTheCurrentRequestContext() {
        MDC.put(RequestIdContext.MDC_KEY, "req-42");
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.7");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        auditService.record(7L, "owner@taskira.test", AuditEntityType.TICKET, 99L, AuditAction.TICKET_CREATED, "TASK-1");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActorId()).isEqualTo(7L);
        assertThat(event.getActorEmail()).isEqualTo("owner@taskira.test");
        assertThat(event.getEntityType()).isEqualTo(AuditEntityType.TICKET);
        assertThat(event.getEntityId()).isEqualTo(99L);
        assertThat(event.getAction()).isEqualTo(AuditAction.TICKET_CREATED);
        assertThat(event.getDetail()).isEqualTo("TASK-1");
        assertThat(event.getRequestId()).isEqualTo("req-42");
        assertThat(event.getIpAddress()).isEqualTo("203.0.113.7");
    }

    @Test
    void recordAcceptsANullActorIdForAnAnonymousEventSuchAsAFailedLogin() {
        auditService.record(null, "attempted@taskira.test", AuditEntityType.AUTH, null, AuditAction.LOGIN_FAILURE, null);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActorId()).isNull();
        assertThat(event.getActorEmail()).isEqualTo("attempted@taskira.test");
        assertThat(event.getEntityId()).isNull();
    }

    @Test
    void recordLeavesRequestIdAndIpAddressNullOutsideAnHttpRequestOrWithoutAnMdcEntry() {
        auditService.record(1L, "user@taskira.test", AuditEntityType.USER, 1L, AuditAction.USER_ACTIVATED, null);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getRequestId()).isNull();
        assertThat(event.getIpAddress()).isNull();
    }

    @Test
    void listEventsClampsAnOutOfRangePageSizeToTheUpperBound() {
        when(auditEventRepository.findAllByOrderByOccurredAtDescIdDesc(any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        auditService.listEvents(0, 5000);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(auditEventRepository).findAllByOrderByOccurredAtDescIdDesc(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void listEventsClampsANegativePageNumberToZero() {
        when(auditEventRepository.findAllByOrderByOccurredAtDescIdDesc(any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        AuditEventPageResponse response = auditService.listEvents(-3, 20);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(auditEventRepository).findAllByOrderByOccurredAtDescIdDesc(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(response.page()).isEqualTo(0);
    }
}
