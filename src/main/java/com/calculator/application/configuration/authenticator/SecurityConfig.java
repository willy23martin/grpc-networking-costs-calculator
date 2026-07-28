package com.calculator.application.configuration.authenticator;

import com.calculator.application.services.authenticator.AuthenticationProviderImpl;
import com.calculator.application.services.authenticator.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity)
    throws Exception {
        httpSecurity
                .authorizeHttpRequests(
                        authorizationManagerRequestMatcherRegistry -> {
                            authorizationManagerRequestMatcherRegistry
                                    .requestMatchers("/auth/**")
                                    .permitAll()
                                    .anyRequest()
                                    .authenticated();
                        }
                )
                .formLogin(
                        formLoginConfigurer -> {
                            formLoginConfigurer
                                    .loginPage("/login")
                                    .permitAll();
                })
                .csrf(AbstractHttpConfigurer::disable)
                .authenticationProvider(
                        authenticationProvider()
                );

        return httpSecurity.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsServiceImpl();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        return new AuthenticationProviderImpl(userDetailsService(), passwordEncoder());
    }

}
