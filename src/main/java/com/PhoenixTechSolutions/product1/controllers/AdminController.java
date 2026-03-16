package com.PhoenixTechSolutions.product1.controllers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.PhoenixTechSolutions.product1.Dtos.UserDto;
import com.PhoenixTechSolutions.product1.model.User;
import com.PhoenixTechSolutions.product1.repositiory.UserRepositiory;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepositiory userRepository;

    public AdminController(UserRepositiory userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/users/{userId}/make-admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Promote a user to admin", description = "Allows an existing admin to promote a regular user to admin status. Requires admin privileges.")

    public ResponseEntity<?> makeAdmin(@PathVariable Long userId, Authentication authentication) {
        User adminUser = (User) authentication.getPrincipal();
        log.info("Admin {} (ID: {}) attempting to make user ID: {} an admin", 
                adminUser.getEmail(), adminUser.getId(), userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        if ("ROLE_ADMIN".equals(user.getRole())) {
            log.warn("Failed attempt - User {} is already an admin", user.getEmail());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "User is already an admin"));
        }
        
        user.setRole("ROLE_ADMIN");
        userRepository.save(user);
        
        log.info("User {} promoted to admin by {}", user.getEmail(), adminUser.getEmail());
        
        return ResponseEntity.ok(Map.of(
            "message", "User is now an admin",
            "email", user.getEmail(),
            "username", user.getUsername()
        ));
    }

    @DeleteMapping("/users/{userId}/remove-admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Remove admin privileges from a user", description = "Allows an existing admin to demote another admin to regular user status. Requires admin privileges. Cannot remove the last remaining admin.")

    public ResponseEntity<?> removeAdmin(@PathVariable Long userId, Authentication authentication) {
        User adminUser = (User) authentication.getPrincipal();
        log.info("Admin {} attempting to remove admin privileges from user ID: {}", 
                adminUser.getEmail(), userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Prevent removing the last admin
        long adminCount = userRepository.findAll().stream()
                .filter(u -> "ROLE_ADMIN".equals(u.getRole()))
                .count();
        
        if (adminCount <= 1 && "ROLE_ADMIN".equals(user.getRole())) {
            log.warn("Failed attempt - Cannot remove the last admin: {}", user.getEmail());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot remove the last admin "));
        }
        
        if (!"ROLE_ADMIN".equals(user.getRole())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "User is not an admin"));
        }
        
        user.setRole("ROLE_USER");
        userRepository.save(user);
        
        log.info("Admin privileges removed from {} by {}", user.getEmail(), adminUser.getEmail());
        
        return ResponseEntity.ok(Map.of(
            "message", "Admin privileges removed",
            "email", user.getEmail()
        ));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get all users", description = "Returns a list of all registered users. Requires admin privileges.")

    public ResponseEntity<List<UserDto>> getAllUsers(Authentication authentication) {
        User adminUser = (User) authentication.getPrincipal();
        log.debug("Admin {} fetching all users", adminUser.getEmail());
        
        List<UserDto> users = userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        log.debug("Retrieved {} users", users.size());
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Convert user to DTO format", description = "Converts a user entity to a DTO format for API responses.")
    private UserDto convertToDto(User user) {
        return new UserDto(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getUsername(),
            user.getRole(),
            user.getCreatedAt()
        );
    }

}