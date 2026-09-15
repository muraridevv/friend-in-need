package com.friendinneed.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, String> register(@Valid @RequestBody Credentials credentials) {
        if (users.existsByUsername(credentials.username()))
            throw new IllegalArgumentException("Username is already registered");
        users.save(new UserEntity(credentials.username(), passwordEncoder.encode(credentials.password())));
        return Map.of("token", jwtService.generateToken(credentials.username()));
    }

    @PostMapping("/login")
    Map<String, String> login(@Valid @RequestBody Credentials credentials) {
        UserEntity user = users.findByUsername(credentials.username())
                .filter(candidate -> passwordEncoder.matches(credentials.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException());
        return Map.of("token", jwtService.generateToken(user.getUsername()));
    }

    record Credentials(@NotBlank @Size(max = 80) String username, @NotBlank @Size(min = 8, max = 128) String password) {
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class InvalidCredentialsException extends RuntimeException {
    }
}
