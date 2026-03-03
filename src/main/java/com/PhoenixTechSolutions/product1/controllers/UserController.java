package com.PhoenixTechSolutions.product1.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.PhoenixTechSolutions.product1.Dtos.LoginRequest;
import com.PhoenixTechSolutions.product1.Dtos.LoginResponse;
import com.PhoenixTechSolutions.product1.Dtos.RegisterRequest;
import com.PhoenixTechSolutions.product1.Security.Jwtutil;
import com.PhoenixTechSolutions.product1.model.User;
import com.PhoenixTechSolutions.product1.repositiory.UserRepositiory;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthenticationManager authenticationManager;
    private final Jwtutil jwtutil;
    private final UserRepositiory userRepositiory;
    private final PasswordEncoder passwordEncoder;

    public UserController(
            AuthenticationManager authenticationManager,
            Jwtutil jwtutil,
            UserRepositiory userRepositiory,
            PasswordEncoder passwordEncoder) {

        this.authenticationManager = authenticationManager;
        this.jwtutil = jwtutil;
        this.userRepositiory = userRepositiory;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().toLowerCase(),
                        request.password()
                )
        );

        User user = userRepositiory
                .findByEmail(authentication.getName())
                .orElseThrow();

        String token = jwtutil.generateToken(user.getUsername());

        return new LoginResponse(
                token,
                user.getUsername()
                    );
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {

        if (userRepositiory.findByEmail(request.email().toLowerCase()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        if (userRepositiory.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already taken");
        }

        User user = User.builder()
                .name(request.name())
                .username(request.username())
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .role("ROLE_USER")
                .build();

        userRepositiory.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

    @GetMapping("/me")
    public Object me(Authentication authentication) {
        return authentication.getPrincipal();
    }
}