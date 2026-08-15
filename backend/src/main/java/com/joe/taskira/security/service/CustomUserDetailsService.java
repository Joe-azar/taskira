package com.joe.taskira.security.service;

import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Deliberately does not check {@code user.isActive()} here: AbstractUserDetailsAuthenticationProvider
     * runs its own preAuthenticationChecks (based on AuthenticatedUser#isEnabled()) right after this
     * method returns, and that path throws a clean DisabledException. Throwing it manually from inside
     * loadUserByUsername() instead gets wrapped by DaoAuthenticationProvider#retrieveUser() into an
     * InternalAuthenticationServiceException, which isn't a DisabledException and falls through to the
     * generic 500 handler instead of the intended 401.
     */
    @Override
    public AuthenticatedUser loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new AuthenticatedUser(user);
    }
}