package com.calculator.application.configuration;

import com.calculator.application.services.authentication.AuthenticationProviderImpl;
import com.calculator.application.services.authentication.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/login", "/", "/error", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated()
                )
                // Enable form login so Spring handles standard login submissions and creates a session
                .formLogin(form -> form
                        .loginPage("/login") // Custom login page URL
                        .loginProcessingUrl("/auth/login") // Endpoint where the login form posts credentials
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                // Because I need to keep basic auth for API tools like Postman
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session
                        // Ensure sessions are created and managed by the server
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .csrf(AbstractHttpConfigurer::disable) // Kept disabled as per your requirement for POST requests
                .authenticationProvider(authenticationProvider());

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