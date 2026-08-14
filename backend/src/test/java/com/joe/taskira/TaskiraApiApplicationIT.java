package com.joe.taskira;

import com.joe.taskira.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TaskiraApiApplicationIT extends PostgreSqlIntegrationTest {

    @Test
    void applicationContextStartsAgainstPostgreSql() {
    }
}
