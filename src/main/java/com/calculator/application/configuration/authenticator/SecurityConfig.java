package com.calculator.application.configuration.authenticator;

import com.calculator.application.services.authenticator.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
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
                .csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsServiceImpl();
    }

}
