package com.joe.taskira.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared PostgreSQL container for integration tests executed by Maven Failsafe.
 *
 * <p>The singleton lifecycle keeps one real database for the complete integration-test
 * phase. Each Spring test context still controls its own transactions and schema checks.</p>
 */
public abstract class PostgreSqlIntegrationTest {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse(
            "postgres:18.6-alpine3.23@sha256:f65cfc1a73466fc0807f4a33496e1c438b19269091c8f998faec871874df8e5e"
    ).asCompatibleSubstituteFor("postgres");

    protected static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>(POSTGRES_IMAGE)
                    .withDatabaseName("taskira_test")
                    .withUsername("taskira_test")
                    .withPassword("taskira_test");

    static {
        POSTGRESQL.start();
    }

    @DynamicPropertySource
    static void configurePostgreSql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
    }
}
