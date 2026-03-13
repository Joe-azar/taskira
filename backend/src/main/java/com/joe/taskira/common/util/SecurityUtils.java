package com.joe.taskira.common.util;

import com.joe.taskira.common.exception.UnauthorizedException;
import com.joe.taskira.security.model.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("User not authenticated");
        }

        return user;
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}