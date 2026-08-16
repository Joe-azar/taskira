package com.joe.taskira.audit.repository;

import com.joe.taskira.audit.entity.AuditEvent;
import com.joe.taskira.audit.enums.AuditAction;
import com.joe.taskira.audit.enums.AuditEntityType;
import com.joe.taskira.common.audit.JpaAuditConfig;
import com.joe.taskira.support.PostgreSqlIntegrationTest;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditConfig.class)
class AuditEventRepositoryIT extends PostgreSqlIntegrationTest {

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savingAnAuditEventStampsOccurredAtAndPersistsAgainstRealPostgreSql() {
        User actor = persistUser("owner@taskira.test");
        AuditEvent event = AuditEvent.builder()
                .actorId(actor.getId())
                .actorEmail(actor.getEmail())
                .entityType(AuditEntityType.TICKET)
                .entityId(99L)
                .action(AuditAction.TICKET_CREATED)
                .detail("TASK-1")
                .requestId("req-42")
                .ipAddress("203.0.113.7")
                .build();

        AuditEvent saved = entityManager.persistFlushFind(event);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOccurredAt()).isNotNull();
        assertThat(saved.getActorId()).isEqualTo(actor.getId());
        assertThat(saved.getActorEmail()).isEqualTo("owner@taskira.test");
        assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.TICKET);
        assertThat(saved.getAction()).isEqualTo(AuditAction.TICKET_CREATED);
        assertThat(saved.getRequestId()).isEqualTo("req-42");
        assertThat(saved.getIpAddress()).isEqualTo("203.0.113.7");
    }

    @Test
    void anAuditEventSurvivesItsActorBeingDeletedBecauseTheForeignKeySetsItNullRatherThanRestrict() {
        User actor = persistUser("to-delete@taskira.test");
        AuditEvent event = entityManager.persistFlushFind(AuditEvent.builder()
                .actorId(actor.getId())
                .actorEmail(actor.getEmail())
                .entityType(AuditEntityType.AUTH)
                .action(AuditAction.LOGIN_SUCCESS)
                .build());

        entityManager.getEntityManager()
                .createQuery("delete from User u where u.id = :id")
                .setParameter("id", actor.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        AuditEvent reloaded = entityManager.find(AuditEvent.class, event.getId());
        assertThat(reloaded.getActorId()).isNull();
        assertThat(reloaded.getActorEmail()).isEqualTo("to-delete@taskira.test");
    }

    @Test
    void anAuditEventWithoutAnActorPersistsWithANullActorIdForAnonymousEventsLikeAFailedLogin() {
        AuditEvent event = AuditEvent.builder()
                .actorId(null)
                .actorEmail("attempted@taskira.test")
                .entityType(AuditEntityType.AUTH)
                .entityId(null)
                .action(AuditAction.LOGIN_FAILURE)
                .build();

        AuditEvent saved = entityManager.persistFlushFind(event);

        assertThat(saved.getActorId()).isNull();
        assertThat(saved.getActorEmail()).isEqualTo("attempted@taskira.test");
    }

    @Test
    void findAllByOrderByOccurredAtDescReturnsTheMostRecentEventFirst() {
        // This table isn't exclusive to this test class - other IT suites commit real rows
        // to the same shared Testcontainers database (unlike @DataJpaTest's own writes here,
        // which roll back after each method). Asking for only the top 2 keeps the assertion
        // valid regardless of what else has already accumulated in the table.
        AuditEvent older = entityManager.persistAndFlush(minimalEvent(AuditAction.LOGIN_SUCCESS));
        AuditEvent newer = entityManager.persistAndFlush(minimalEvent(AuditAction.LOGOUT));
        entityManager.clear();

        var page = auditEventRepository.findAllByOrderByOccurredAtDescIdDesc(PageRequest.of(0, 2));

        assertThat(page.getContent())
                .extracting(AuditEvent::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    private AuditEvent minimalEvent(AuditAction action) {
        return AuditEvent.builder()
                .actorId(null)
                .actorEmail("user@taskira.test")
                .entityType(AuditEntityType.AUTH)
                .action(action)
                .build();
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
}
