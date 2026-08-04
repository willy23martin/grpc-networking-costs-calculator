package com.calculator.infrastructure.web.rest.authentication;

import com.calculator.application.services.authentication.UserRegistrationService;
import com.calculator.domain.dto.authentication.RegisterDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
public class UserAuthenticationController {

    private final Logger log = Logger.getLogger(UserAuthenticationController.class.getName());

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserRegistrationService userAuthenticatorService;

    @GetMapping("/sec-init")
    public String secInit() {
        log.info("GET API call sec-init");
        return "sec-init";
    }

    @PostMapping("/auth/register")
    public void register(
            @RequestBody RegisterDTO registerDTO,
            HttpServletRequest httpServletRequest
    ) throws Exception {
        log.info("POST API call register");
        userAuthenticatorService.register(registerDTO);
    }


}