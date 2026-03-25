package com.PhoenixTechSolutions.product1.controllers;

import com.PhoenixTechSolutions.product1.Dtos.LoginRequest;
import com.PhoenixTechSolutions.product1.Dtos.RegisterRequest;
import com.PhoenixTechSolutions.product1.Security.Jwtutil;
import com.PhoenixTechSolutions.product1.model.User;
import com.PhoenixTechSolutions.product1.repositiory.UserRepositiory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthenticationManager authenticationManager;
    private final Jwtutil jwtutil;
    private final UserRepositiory userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(
            AuthenticationManager authenticationManager,
            Jwtutil jwtutil,
            UserRepositiory userRepository,
            PasswordEncoder passwordEncoder) {

        this.authenticationManager = authenticationManager;
        this.jwtutil = jwtutil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates a user and returns a JWT token along with user details.")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email().toLowerCase(),
                            request.password()
                    )
            );

            User user = userRepository
                    .findByEmail(authentication.getName())
                    .orElseThrow();

            String token = jwtutil.generateToken(user.getEmail());
            
            // Clean the name - remove newlines
            String cleanName = user.getName() != null ? 
                user.getName().replaceAll("[\n\r]", " ").trim() : "";

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("name", cleanName);  // ← Use cleanName here, NOT user.getName()

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Creates a new user account with the provided details. The first user registered will be granted ADMIN privileges.")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        
        // Check if email already exists
        if (userRepository.findByEmail(request.email().toLowerCase()).isPresent()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email already exists");
            return ResponseEntity.badRequest().body(error);
        }

        // Check if username already taken
        if (userRepository.findByUsername(request.username()).isPresent()) {
            log.warn("Registration failed - Username already taken: {}", request.username());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Username already taken");
            return ResponseEntity.badRequest().body(error);
        }

        // Check if this is the first user EVER
        long userCount = userRepository.count();
        boolean isFirstUser = userCount == 0;

        // Create new user
        User user = User.builder()
                .name(request.name())
                .username(request.username())
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .role(isFirstUser ? "ROLE_ADMIN" : "ROLE_USER")
                .createdAt(java.time.LocalDateTime.now())  // Set createdAt to current time
                .build();

        User savedUser = userRepository.save(user);

        Map<String, String> success = new HashMap<>();
            success.put("message", "User registered successfully");
            success.put("email", user.getEmail());
            success.put("username", user.getUsername());
            success.put("role", user.getRole());
    
            if (isFirstUser) {
                success.put("warning", "IMPORTANT: You are the first user and have been granted ADMIN privileges. " +
                                "You can now create other admins through the admin panel.");
            
                log.info("FIRST USER REGISTERED AS ADMIN: {}", savedUser.getEmail());
            }
         return ResponseEntity.ok(success); 
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user details", description = "Returns the details of the authenticated user. Requires authentication.")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) {
            log.warn("Unauthorized access to /me endpoint");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        User user = (User) authentication.getPrincipal();
        log.debug("User details accessed: {}", user.getEmail());

        
        // Clean the name
        String cleanName = user.getName() != null ? 
        user.getName().replaceAll("[\n\r]", " ").trim() : "";
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("name", cleanName);
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("role", user.getAuthorities());
        response.put("createdAt", user.getCreatedAt());
        
        return ResponseEntity.ok(response);
    }
}