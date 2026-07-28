package com.calculator.application.services.authenticator;

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
        verifyUser(registerDTO);

        persistRegisteredUser(registerDTO);
    }

    private void verifyUser(RegisterDTO registerDTO) throws Exception {
        registerByUsername(registerDTO.username()).ifPresent(userEntity -> {
            throw new RuntimeException("Username already exists");
        });

        registerByEmail(registerDTO.email())
                .ifPresent( userEntity -> {
                    throw new RuntimeException("Username already exists");
                });
    }

    private void persistRegisteredUser(RegisterDTO registerDTO) {
        String encodedPassword = passwordEncoder.encode(registerDTO.password());
        UserEntity persistedUserEntity = new UserEntity(
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
