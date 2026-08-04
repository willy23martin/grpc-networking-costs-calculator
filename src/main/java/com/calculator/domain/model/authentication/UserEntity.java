package com.calculator.domain.model.authenticator;

import com.calculator.domain.model.authentication.UserAuthority;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@ToString
public class UserEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private List<UserAuthority> userAuthorities = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    private Set<String> ips = new HashSet<>();

    public UserEntity(
            String username,
            String password,
            String email
    ) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.userAuthorities.add(UserAuthority.ROLE_USER);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.userAuthorities.stream()
                .map(userAuthority ->  new SimpleGrantedAuthority(userAuthority.toString())
                ).toList();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }
}