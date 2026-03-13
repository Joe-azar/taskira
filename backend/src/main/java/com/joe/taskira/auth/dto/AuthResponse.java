package com.joe.taskira.auth.dto;

import com.joe.taskira.user.entity.User;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        MeResponse user
) {
    public static AuthResponse of(String token, long expiresIn, User user) {
        return new AuthResponse(
                token,
                "Bearer",
                expiresIn,
                MeResponse.from(user)
        );
    }
}