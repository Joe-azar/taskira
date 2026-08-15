package com.joe.taskira.project.repository;

import com.joe.taskira.common.audit.JpaAuditConfig;
import com.joe.taskira.project.entity.Project;
import com.joe.taskira.project.entity.ProjectMember;
import com.joe.taskira.project.enums.ProjectRole;
import com.joe.taskira.project.enums.ProjectStatus;
import com.joe.taskira.support.PostgreSqlIntegrationTest;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditConfig.class)
class ProjectRepositoryIT extends PostgreSqlIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    void flywayAppliesEveryMigrationToPostgreSql() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }

        assertThat(flyway.info().applied())
                .extracting(migration -> migration.getVersion().toString())
                .containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
    }

    @Test
    void accessibleProjectsUseRealPostgreSqlQueriesWithoutDuplicates() {
        User firstOwner = persistUser("owner.one@taskira.test");
        User secondOwner = persistUser("owner.two@taskira.test");
        User member = persistUser("member@taskira.test");

        Project ownedProject = persistProject("ALPHA", "Alpha", member);
        Project memberProject = persistProject("BETA", "Beta", secondOwner);
        persistProject("PRIVATE", "Private", secondOwner);

        persistMembership(ownedProject, member, ProjectRole.OWNER);
        persistMembership(ownedProject, firstOwner, ProjectRole.MEMBER);
        persistMembership(ownedProject, secondOwner, ProjectRole.MANAGER);
        persistMembership(memberProject, member, ProjectRole.MANAGER);
        entityManager.flush();
        entityManager.clear();

        assertThat(projectRepository.existsByCodeIgnoreCase("alpha")).isTrue();
        assertThat(projectRepository.findAccessibleProjects(member.getId()))
                .extracting(Project::getCode)
                .containsExactly("ALPHA", "BETA");
        assertThat(projectRepository.findAccessibleProjectIds(member.getId()))
                .containsExactly(ownedProject.getId(), memberProject.getId());
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

    private Project persistProject(String code, String name, User owner) {
        Project project = Project.builder()
                .code(code)
                .name(name)
                .description(null)
                .owner(owner)
                .status(ProjectStatus.ACTIVE)
                .ticketSequence(0)
                .build();
        return entityManager.persistAndFlush(project);
    }

    private void persistMembership(Project project, User user, ProjectRole role) {
        ProjectMember membership = ProjectMember.builder()
                .project(project)
                .user(user)
                .projectRole(role)
                .build();
        entityManager.persist(membership);
    }
}
