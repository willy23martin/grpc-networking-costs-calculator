package com.calculator.features;

import com.calculator.domain.model.authenticator.UserAuthority;
import io.cucumber.java.Before;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public class CucumberSecurityHooks {

    @Before
    public void setupSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "cucumberTestUser",
                        "password",
                        List.of(new SimpleGrantedAuthority(UserAuthority.ROLE_USER.name()))
                )
        );
    }
}