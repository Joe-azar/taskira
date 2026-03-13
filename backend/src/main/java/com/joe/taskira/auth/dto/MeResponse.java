package com.joe.taskira.auth.dto;

import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;

public record MeResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        GlobalRole globalRole,
        boolean active
) {
    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getEmail(),
                user.getGlobalRole(),
                user.isActive()
        );
    }
}