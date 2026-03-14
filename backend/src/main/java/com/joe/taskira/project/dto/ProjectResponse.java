package com.joe.taskira.project.dto;

import com.joe.taskira.project.entity.Project;
import com.joe.taskira.project.enums.ProjectStatus;
import com.joe.taskira.user.dto.UserSummaryResponse;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String code,
        String name,
        String description,
        ProjectStatus status,
        UserSummaryResponse owner,
        long memberCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse from(Project project, long memberCount) {
        return new ProjectResponse(
                project.getId(),
                project.getCode(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                UserSummaryResponse.from(project.getOwner()),
                memberCount,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}