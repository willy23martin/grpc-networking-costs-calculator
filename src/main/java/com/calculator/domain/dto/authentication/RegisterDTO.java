package com.calculator.domain.dto.authentication;

import com.calculator.domain.model.authenticator.UserEntity;

public record RegisterDTO(
        String username,
        String password,
        String email
){
}