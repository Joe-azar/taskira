package com.joe.taskira;

import com.joe.taskira.security.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class TaskiraApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskiraApiApplication.class, args);
    }
}