package com.friendlypoker.service;

import com.friendlypoker.dto.AuthResponse;
import com.friendlypoker.dto.LoginRequest;
import com.friendlypoker.dto.RegisterRequest;
import com.friendlypoker.model.User;
import com.friendlypoker.repository.UserRepository;
import com.friendlypoker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(req.username()), req.username());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return new AuthResponse(jwtService.generateToken(req.username()), req.username());
    }
}
