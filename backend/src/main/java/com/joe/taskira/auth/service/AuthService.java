package com.joe.taskira.auth.service;

import com.joe.taskira.audit.enums.AuditAction;
import com.joe.taskira.audit.enums.AuditEntityType;
import com.joe.taskira.audit.service.AuditService;
import com.joe.taskira.auth.dto.LoginRequest;
import com.joe.taskira.auth.dto.MeResponse;
import com.joe.taskira.auth.dto.RegisterRequest;
import com.joe.taskira.common.exception.ConflictException;
import com.joe.taskira.common.exception.UnauthorizedException;
import com.joe.taskira.common.util.SecurityUtils;
import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    public MeResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (!request.password().equals(request.confirmPassword())) {
            throw new ConflictException("Password confirmation does not match");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already in use");
        }

        User user = User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .globalRole(GlobalRole.USER)
                .active(true)
                .build();

        user = userRepository.save(user);

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser, null, authenticatedUser.getAuthorities()
                )
        );

        return MeResponse.from(user);
    }

    public MeResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (AuthenticationException ex) {
            auditService.record(null, email, AuditEntityType.AUTH, null, AuditAction.LOGIN_FAILURE, null);
            throw ex;
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof AuthenticatedUser authenticatedUser)) {
            throw new UnauthorizedException("Authentication failed");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        auditService.record(
                authenticatedUser.getId(),
                authenticatedUser.getUser().getEmail(),
                AuditEntityType.AUTH,
                null,
                AuditAction.LOGIN_SUCCESS,
                null
        );

        return MeResponse.from(authenticatedUser.getUser());
    }

    public MeResponse me() {
        return MeResponse.from(SecurityUtils.getCurrentUser().getUser());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
