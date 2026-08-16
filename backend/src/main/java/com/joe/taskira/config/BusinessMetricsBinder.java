package com.joe.taskira.config;

import com.joe.taskira.project.enums.ProjectStatus;
import com.joe.taskira.project.repository.ProjectRepository;
import com.joe.taskira.ticket.enums.TicketStatus;
import com.joe.taskira.ticket.repository.TicketRepository;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Registers business-level gauges alongside the JVM/HTTP metrics Micrometer's
 * autoconfiguration already binds. Each gauge re-queries its repository (an indexed
 * count, see the tickets/projects status indexes) on every Prometheus scrape rather than
 * caching a snapshot - staleness between scrapes would defeat the point of a dashboard.
 * <p>
 * Named without a ".total" suffix on purpose: Micrometer's PrometheusNamingConvention
 * reserves "_total" for counters and silently strips it from gauge names, so a name here
 * ending in ".total" would render as "taskira_tickets" anyway - keeping the Java-side name
 * and the actual scraped metric name identical avoids that mismatch surprising a reader.
 */
@Component
@RequiredArgsConstructor
public class BusinessMetricsBinder implements MeterBinder {

    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    public void bindTo(MeterRegistry registry) {
        for (TicketStatus status : TicketStatus.values()) {
            Gauge.builder("taskira.tickets", ticketRepository, repo -> repo.countByStatus(status))
                    .tag("status", status.name())
                    .description("Number of tickets currently in this status")
                    .register(registry);
        }

        for (ProjectStatus status : ProjectStatus.values()) {
            Gauge.builder("taskira.projects", projectRepository, repo -> repo.countByStatus(status))
                    .tag("status", status.name())
                    .description("Number of projects currently in this status")
                    .register(registry);
        }

        for (GlobalRole role : GlobalRole.values()) {
            Gauge.builder("taskira.users.active", userRepository, repo -> repo.countByGlobalRoleAndActiveTrue(role))
                    .tag("role", role.name())
                    .description("Number of active users holding this global role")
                    .register(registry);
        }
    }
}
