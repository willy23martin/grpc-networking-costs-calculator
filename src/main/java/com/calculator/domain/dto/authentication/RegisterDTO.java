package com.calculator.domain.dto.authentication;

public record RegisterDTO(
        String username,
        String password,
        String email
){
}