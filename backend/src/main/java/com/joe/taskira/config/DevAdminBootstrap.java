package com.joe.taskira.config;

import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import com.joe.taskira.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every path that grants the ADMIN role (UserService#createUser, #updateUser) requires an
 * already-authenticated ADMIN caller, so a fresh database has no way to produce its first
 * admin short of a manual SQL insert. This runner exists only to break that bootstrap
 * problem in local development. It is restricted to the "dev" Spring profile - it never
 * runs under "prod" (or any other profile, including the plain default profile the E2E and
 * test stacks run under) - and is idempotent by email, so restarting the dev stack any
 * number of times neither duplicates the account nor resets its password if an admin has
 * since changed it.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevAdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.dev-admin.email:admin@taskira.test}")
    private String email;

    @Value("${app.dev-admin.password:Taskira-Admin-42!}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }

        User admin = User.builder()
                .firstName("Taskira")
                .lastName("Admin")
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(password))
                .globalRole(GlobalRole.ADMIN)
                .active(true)
                .build();

        userRepository.save(admin);
        log.warn("Dev-only bootstrap created a default admin account ({}). "
                + "This runner is active only under the 'dev' profile and must never reach production.", normalizedEmail);
    }
}
