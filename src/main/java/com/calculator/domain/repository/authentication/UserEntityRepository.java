package com.calculator.domain.repository.authentication;

import com.calculator.domain.model.authenticator.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserEntityRepository extends JpaRepository<UserEntity, Long> {

    @EntityGraph(attributePaths = {"userAuthorities", "ips"})
    Optional<UserEntity> findByUsername(String username);

    @EntityGraph(attributePaths = {"userAuthorities", "ips"})
    Optional<UserEntity> findByEmail(String email);

}