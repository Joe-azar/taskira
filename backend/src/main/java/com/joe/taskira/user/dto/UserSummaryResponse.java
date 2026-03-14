package com.joe.taskira.user.dto;

import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;

public record UserSummaryResponse(
        Long id,
        String fullName,
        String email,
        GlobalRole globalRole,
        boolean active
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getGlobalRole(),
                user.isActive()
        );
    }
}