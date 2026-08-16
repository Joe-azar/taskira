package com.joe.taskira.ticket.repository;

import com.joe.taskira.common.audit.JpaAuditConfig;
import com.joe.taskira.project.entity.Project;
import com.joe.taskira.project.enums.ProjectStatus;
import com.joe.taskira.support.PostgreSqlIntegrationTest;
import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.enums.TicketPriority;
import com.joe.taskira.ticket.enums.TicketStatus;
import com.joe.taskira.ticket.enums.TicketType;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditConfig.class)
class TicketRepositoryIT extends PostgreSqlIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void countByStatusCountsOnlyTicketsInThatStatus() {
        // Other integration test classes commit real rows to this same shared Testcontainers
        // database (unlike this @DataJpaTest, which rolls back), so the table is never empty
        // here - assert the delta this test itself causes, not an absolute count.
        long openBefore = ticketRepository.countByStatus(TicketStatus.OPEN);
        long doneBefore = ticketRepository.countByStatus(TicketStatus.DONE);

        User user = persistUser("reporter@taskira.test");
        Project project = persistProject(user);
        persistTicket(project, user, "PROJ-1", TicketStatus.OPEN);
        persistTicket(project, user, "PROJ-2", TicketStatus.OPEN);
        persistTicket(project, user, "PROJ-3", TicketStatus.DONE);
        entityManager.clear();

        assertThat(ticketRepository.countByStatus(TicketStatus.OPEN)).isEqualTo(openBefore + 2);
        assertThat(ticketRepository.countByStatus(TicketStatus.DONE)).isEqualTo(doneBefore + 1);
    }

    private User persistUser(String email) {
        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .passwordHash("encoded-password")
                .globalRole(GlobalRole.USER)
                .active(true)
                .build();
        return entityManager.persistAndFlush(user);
    }

    private Project persistProject(User owner) {
        Project project = Project.builder()
                .code("PROJ")
                .name("Project")
                .description(null)
                .owner(owner)
                .status(ProjectStatus.ACTIVE)
                .ticketSequence(0)
                .build();
        return entityManager.persistAndFlush(project);
    }

    private void persistTicket(Project project, User user, String reference, TicketStatus status) {
        Ticket ticket = Ticket.builder()
                .reference(reference)
                .project(project)
                .title("Ticket " + reference)
                .description(null)
                .type(TicketType.TASK)
                .status(status)
                .priority(TicketPriority.MEDIUM)
                .creator(user)
                .assignee(null)
                .dueDate(null)
                .build();
        entityManager.persistAndFlush(ticket);
    }
}
