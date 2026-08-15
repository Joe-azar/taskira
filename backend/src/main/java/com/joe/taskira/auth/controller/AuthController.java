package com.joe.taskira.auth.controller;

import com.joe.taskira.auth.dto.AuthResponse;
import com.joe.taskira.auth.dto.LoginRequest;
import com.joe.taskira.auth.dto.MeResponse;
import com.joe.taskira.auth.dto.RegisterRequest;
import com.joe.taskira.auth.service.AuthService;
import com.joe.taskira.common.web.ApiVersion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.register(request);
        persistSession(httpRequest, httpResponse);
        return response;
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.login(request);
        persistSession(httpRequest, httpResponse);
        return response;
    }

    @GetMapping("/me")
    public MeResponse me() {
        return authService.me();
    }

    private void persistSession(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        securityContextRepository.saveContext(
                SecurityContextHolder.getContext(),
                httpRequest,
                httpResponse
        );
    }
}