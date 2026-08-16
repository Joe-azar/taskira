package com.joe.taskira.user.service;

import com.joe.taskira.audit.enums.AuditAction;
import com.joe.taskira.audit.enums.AuditEntityType;
import com.joe.taskira.audit.service.AuditService;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

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
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

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

        auditService.record(
                currentUser.getId(),
                currentUser.getUser().getEmail(),
                AuditEntityType.USER,
                saved.getId(),
                AuditAction.USER_CREATED,
                saved.getEmail() + " (" + saved.getGlobalRole() + ")"
        );

        return UserResponse.from(saved);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        ensureAdmin();

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String normalizedEmail = normalizeEmail(request.email());
        if (!existing.getEmail().equalsIgnoreCase(normalizedEmail)
                && userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email is already in use");
        }

        if (existing.getId().equals(currentUser.getId())) {
            if (request.active() != null && !request.active()) {
                throw new ConflictException("An administrator cannot deactivate themselves");
            }
            if (request.globalRole() != null && request.globalRole() != GlobalRole.ADMIN) {
                throw new ConflictException("An administrator cannot remove their own admin role");
            }
        }

        if (existing.getGlobalRole() == GlobalRole.ADMIN
                && request.globalRole() != null
                && request.globalRole() != GlobalRole.ADMIN
                && userRepository.countByGlobalRoleAndActiveTrue(GlobalRole.ADMIN) <= 1) {
            throw new ConflictException("Cannot demote the last active admin");
        }

        if (existing.getGlobalRole() == GlobalRole.ADMIN
                && existing.isActive()
                && request.active() != null
                && !request.active()
                && userRepository.countByGlobalRoleAndActiveTrue(GlobalRole.ADMIN) <= 1) {
            throw new ConflictException("Cannot deactivate the last active admin");
        }

        GlobalRole oldRole = existing.getGlobalRole();
        boolean oldActive = existing.isActive();

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

        recordUpdateUserAudit(currentUser, saved, oldRole, oldActive, request);

        return UserResponse.from(saved);
    }

    private void recordUpdateUserAudit(
            AuthenticatedUser currentUser,
            User saved,
            GlobalRole oldRole,
            boolean oldActive,
            UpdateUserRequest request
    ) {
        if (request.globalRole() != null && request.globalRole() != oldRole) {
            auditService.record(
                    currentUser.getId(),
                    currentUser.getUser().getEmail(),
                    AuditEntityType.USER,
                    saved.getId(),
                    AuditAction.USER_ROLE_CHANGED,
                    oldRole + " -> " + saved.getGlobalRole()
            );
        }
        if (request.active() != null && request.active() != oldActive) {
            recordActivationChange(currentUser, saved);
        }
    }

    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        ensureAdmin();

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (existing.getId().equals(currentUser.getId()) && !request.active()) {
            throw new ConflictException("An administrator cannot deactivate themselves");
        }

        if (existing.getGlobalRole() == GlobalRole.ADMIN
                && !request.active()
                && userRepository.countByGlobalRoleAndActiveTrue(GlobalRole.ADMIN) <= 1) {
            throw new ConflictException("Cannot deactivate the last active admin");
        }

        boolean oldActive = existing.isActive();
        existing.setActive(request.active());
        User saved = userRepository.save(existing);

        if (request.active() != oldActive) {
            recordActivationChange(currentUser, saved);
        }

        return UserResponse.from(saved);
    }

    private void recordActivationChange(AuthenticatedUser currentUser, User target) {
        auditService.record(
                currentUser.getId(),
                currentUser.getUser().getEmail(),
                AuditEntityType.USER,
                target.getId(),
                target.isActive() ? AuditAction.USER_ACTIVATED : AuditAction.USER_DEACTIVATED,
                null
        );
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
