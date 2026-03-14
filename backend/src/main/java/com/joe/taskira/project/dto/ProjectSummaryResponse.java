package com.joe.taskira.project.dto;

import com.joe.taskira.project.entity.Project;
import com.joe.taskira.project.enums.ProjectStatus;

public record ProjectSummaryResponse(
        Long id,
        String code,
        String name,
        ProjectStatus status
) {
    public static ProjectSummaryResponse from(Project project) {
        return new ProjectSummaryResponse(
                project.getId(),
                project.getCode(),
                project.getName(),
                project.getStatus()
        );
    }
}