package com.joe.taskira.config;

import com.joe.taskira.project.enums.ProjectStatus;
import com.joe.taskira.project.repository.ProjectRepository;
import com.joe.taskira.ticket.enums.TicketStatus;
import com.joe.taskira.ticket.repository.TicketRepository;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BusinessMetricsBinderTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    private SimpleMeterRegistry registry;

    @BeforeEach
    void bindGauges() {
        lenient().when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(3L);
        lenient().when(ticketRepository.countByStatus(TicketStatus.DONE)).thenReturn(5L);
        lenient().when(projectRepository.countByStatus(ProjectStatus.ACTIVE)).thenReturn(2L);
        lenient().when(userRepository.countByGlobalRoleAndActiveTrue(GlobalRole.ADMIN)).thenReturn(1L);

        registry = new SimpleMeterRegistry();
        new BusinessMetricsBinder(ticketRepository, projectRepository, userRepository).bindTo(registry);
    }

    @Test
    void ticketGaugeReflectsTheRepositoryCountForEachStatusTag() {
        assertThat(registry.get("taskira.tickets.total").tag("status", "OPEN").gauge().value()).isEqualTo(3.0);
        assertThat(registry.get("taskira.tickets.total").tag("status", "DONE").gauge().value()).isEqualTo(5.0);
    }

    @Test
    void projectGaugeReflectsTheRepositoryCountForEachStatusTag() {
        assertThat(registry.get("taskira.projects.total").tag("status", "ACTIVE").gauge().value()).isEqualTo(2.0);
    }

    @Test
    void userGaugeReflectsTheRepositoryCountForEachRoleTag() {
        assertThat(registry.get("taskira.users.active").tag("role", "ADMIN").gauge().value()).isEqualTo(1.0);
    }

    @Test
    void everyTicketStatusProjectStatusAndGlobalRoleIsRegisteredAsItsOwnGauge() {
        assertThat(registry.find("taskira.tickets.total").gauges()).hasSize(TicketStatus.values().length);
        assertThat(registry.find("taskira.projects.total").gauges()).hasSize(ProjectStatus.values().length);
        assertThat(registry.find("taskira.users.active").gauges()).hasSize(GlobalRole.values().length);
    }
}
