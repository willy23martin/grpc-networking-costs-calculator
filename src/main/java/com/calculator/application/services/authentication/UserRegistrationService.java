package com.calculator.application.services.authentication;

import com.calculator.domain.dto.authentication.RegisterDTO;
import com.calculator.domain.model.authenticator.UserEntity;
import com.calculator.domain.repository.authentication.UserEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.logging.Logger;

@Service
public class UserRegistrationService {

    private final Logger log = Logger.getLogger(UserRegistrationService.class.getName());

    @Autowired
    private UserEntityRepository userEntityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void register(RegisterDTO registerDTO) throws Exception{
        log.info("Registering: " + registerDTO.toString());
        verifyUser(registerDTO);

        persistRegisteredUser(registerDTO);
        log.info("User has been registered.");
    }

    private void verifyUser(RegisterDTO registerDTO) throws Exception {
        log.info("Verifying user: " + registerDTO.toString());
        registerByUsername(registerDTO.username()).ifPresent(userEntity -> {
            throw new RuntimeException("Username already exists");
        });

        registerByEmail(registerDTO.email())
                .ifPresent( userEntity -> {
                    throw new RuntimeException("Username already exists");
                });
        log.info("User has been verified: " + registerDTO);
    }

    private void persistRegisteredUser(RegisterDTO registerDTO) {
        log.info("Persisting registered user: " + registerDTO);
        final String encodedPassword = passwordEncoder.encode(registerDTO.password());
        final UserEntity persistedUserEntity = new UserEntity(
                registerDTO.username(),
                encodedPassword,
                registerDTO.email()
        );
        userEntityRepository.save(persistedUserEntity);
        log.info("Registered user: " + persistedUserEntity);
    }

    private Optional<UserEntity> registerByUsername(String username) throws Exception{
        return userEntityRepository
                .findByUsername(username);
    }

    private Optional<UserEntity> registerByEmail(String email) throws Exception{
        return userEntityRepository
                .findByEmail(email);
    }
}