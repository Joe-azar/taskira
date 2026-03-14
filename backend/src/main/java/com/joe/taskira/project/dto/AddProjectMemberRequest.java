package com.joe.taskira.project.dto;

import com.joe.taskira.project.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record AddProjectMemberRequest(

        @NotNull(message = "User id is required")
        Long userId,

        @NotNull(message = "Project role is required")
        ProjectRole projectRole
) {
}