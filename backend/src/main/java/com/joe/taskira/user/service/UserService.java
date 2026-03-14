package com.joe.taskira.user.service;

import com.joe.taskira.common.exception.ForbiddenException;
import com.joe.taskira.common.exception.ResourceNotFoundException;
import com.joe.taskira.common.util.SecurityUtils;
import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.user.dto.UserResponse;
import com.joe.taskira.user.dto.UserSummaryResponse;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getCurrentUser() {
        return UserResponse.from(SecurityUtils.getCurrentUser().getUser());
    }

    public List<UserSummaryResponse> listUsers(String search) {
        List<User> users = (search == null || search.isBlank())
                ? userRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc()
                : userRepository.searchActiveUsers(search.trim());

        return users.stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isAdmin = currentUser.getUser().getGlobalRole() == GlobalRole.ADMIN;
        boolean isSelf = currentUser.getId().equals(id);

        if (!isAdmin && !isSelf) {
            throw new ForbiddenException("You are not allowed to access this user");
        }

        return UserResponse.from(targetUser);
    }
}