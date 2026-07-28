package com.calculator.application.services.authenticator;

import com.calculator.domain.repository.authentication.UserEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserEntityRepository userEntityRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.userEntityRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new UsernameNotFoundException("User not found.")
                );
    }
}
