package com.joe.taskira.user.service;

import com.joe.taskira.common.exception.ConflictException;
import com.joe.taskira.common.exception.ForbiddenException;
import com.joe.taskira.common.exception.ResourceNotFoundException;
import com.joe.taskira.common.util.SecurityUtils;
import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.user.dto.CreateUserRequest;
import com.joe.taskira.user.dto.UpdateUserRequest;
import com.joe.taskira.user.dto.UpdateUserStatusRequest;
import com.joe.taskira.user.dto.UserResponse;
import com.joe.taskira.user.dto.UserSummaryResponse;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getCurrentUser() {
        return UserResponse.from(SecurityUtils.getCurrentUser().getUser());
    }

    public List<UserSummaryResponse> listUsers(String search) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();
        boolean isAdmin = currentUser.getUser().getGlobalRole() == GlobalRole.ADMIN;

        List<User> users;
        if (search == null || search.isBlank()) {
            users = isAdmin
                    ? userRepository.findAllByOrderByFirstNameAscLastNameAsc()
                    : userRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc();
        } else {
            String query = search.trim();
            users = isAdmin
                    ? userRepository.searchAllUsers(query)
                    : userRepository.searchActiveUsers(query);
        }

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

    public UserResponse createUser(CreateUserRequest request) {
        ensureAdmin();

        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email is already in use");
        }

        User user = User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .globalRole(request.globalRole())
                .active(request.active())
                .build();

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        ensureAdmin();

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String normalizedEmail = normalizeEmail(request.email());
        if (!existing.getEmail().equalsIgnoreCase(normalizedEmail)
                && userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email is already in use");
        }

        existing.setFirstName(request.firstName().trim());
        existing.setLastName(request.lastName().trim());
        existing.setEmail(normalizedEmail);
        if (request.globalRole() != null) {
            existing.setGlobalRole(request.globalRole());
        }
        if (request.active() != null) {
            existing.setActive(request.active());
        }

        User saved = userRepository.save(existing);
        return UserResponse.from(saved);
    }

    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        ensureAdmin();

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        existing.setActive(request.active());
        User saved = userRepository.save(existing);
        return UserResponse.from(saved);
    }

    private void ensureAdmin() {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null || currentUser.getUser().getGlobalRole() != GlobalRole.ADMIN) {
            throw new ForbiddenException("Admin role required");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
