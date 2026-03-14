package com.joe.taskira.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(

        @NotBlank(message = "Project code is required")
        @Size(min = 2, max = 20, message = "Project code must contain between 2 and 20 characters")
        String code,

        @NotBlank(message = "Project name is required")
        @Size(max = 150, message = "Project name must not exceed 150 characters")
        String name,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description
) {
}