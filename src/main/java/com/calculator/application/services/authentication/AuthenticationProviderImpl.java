package com.calculator.application.services.authentication;

import com.calculator.infrastructure.web.rest.TCOUnitEconomicsController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.logging.Logger;

public class AuthenticationProviderImpl implements AuthenticationProvider {

    private static final Logger log = Logger.getLogger(AuthenticationProviderImpl.class.getName());

    private final UserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthenticationProviderImpl(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.info("Authenticating: " + authentication.getName());
        final String username = authentication.getName();
        final String password = authentication.getCredentials().toString();

        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        final String storedPassword = userDetails.getPassword();

        if(!isValidPassword(password, storedPassword)) {
            throw new BadCredentialsException("Invalid credentials.");
        }

        return new UsernamePasswordAuthenticationToken(
                userDetails.getUsername(),
                userDetails.getPassword(),
                userDetails.getAuthorities()
        );
    }

    private boolean isValidPassword(String password, String storedPassword) {
        return passwordEncoder.matches(password, storedPassword); // Because we dont need to decode it.
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}