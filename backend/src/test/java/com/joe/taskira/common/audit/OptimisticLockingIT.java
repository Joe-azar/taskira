package com.joe.taskira.common.audit;

import com.joe.taskira.project.entity.Project;
import com.joe.taskira.project.enums.ProjectStatus;
import com.joe.taskira.project.repository.ProjectRepository;
import com.joe.taskira.support.PostgreSqlIntegrationTest;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditConfig.class)
class OptimisticLockingIT extends PostgreSqlIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savingAStaleCopyAfterAConcurrentUpdateThrowsOptimisticLockingFailure() {
        User owner = entityManager.persistAndFlush(
                User.builder()
                        .firstName("Owner")
                        .lastName("Test")
                        .email("optimistic-locking-owner@taskira.test")
                        .passwordHash("encoded-password")
                        .globalRole(GlobalRole.USER)
                        .active(true)
                        .build()
        );
        Long projectId = entityManager.persistAndFlush(
                Project.builder()
                        .code("OPTLOCK")
                        .name("Original name")
                        .description(null)
                        .owner(owner)
                        .status(ProjectStatus.ACTIVE)
                        .ticketSequence(0)
                        .build()
        ).getId();
        entityManager.clear();

        Project staleCopy = projectRepository.findById(projectId).orElseThrow();
        entityManager.detach(staleCopy);
        Project freshCopy = projectRepository.findById(projectId).orElseThrow();

        assertThat(staleCopy.getVersion()).isEqualTo(freshCopy.getVersion());

        freshCopy.setName("Updated concurrently");
        projectRepository.saveAndFlush(freshCopy);

        staleCopy.setName("Updated from a stale read");
        assertThatThrownBy(() -> projectRepository.saveAndFlush(staleCopy))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
