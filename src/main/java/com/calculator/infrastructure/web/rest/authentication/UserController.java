package com.calculator.infrastructure.web.rest.authentication;

import com.calculator.application.services.authenticator.UserRegistrationService;
import com.calculator.domain.dto.authentication.RegisterDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    UserRegistrationService userAuthenticatorService;

    @PostMapping("/auth/register")
    public void register(
            @RequestBody RegisterDTO registerDTO,
            HttpServletRequest httpServletRequest
    ) throws Exception {
        userAuthenticatorService.register(registerDTO);
    }


}
