package com.joe.taskira.user.dto;

import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;

import java.time.Instant;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        GlobalRole globalRole,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getEmail(),
                user.getGlobalRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}