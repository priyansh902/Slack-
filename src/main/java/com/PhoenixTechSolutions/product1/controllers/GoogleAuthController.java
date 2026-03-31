package com.PhoenixTechSolutions.product1.controllers;

import com.PhoenixTechSolutions.product1.Security.Jwtutil;
import com.PhoenixTechSolutions.product1.model.User;
import com.PhoenixTechSolutions.product1.repositiory.UserRepositiory;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {

    private final UserRepositiory userRepository;
    private final Jwtutil jwtutil;
    private final PasswordEncoder passwordEncoder;

    public GoogleAuthController(
            UserRepositiory userRepository,
            Jwtutil jwtutil,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtutil = jwtutil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * POST /api/auth/google
     * Body: { "idToken": "<google_id_token>" }
     *
     * Verifies the Google ID token, finds or creates the user,
     * and returns the same JWT structure as /api/users/login
     */
    @PostMapping("/google")
    @Operation(summary = "Google Sign-In", description = "Verify Google ID token and return app JWT")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");

        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "idToken is required"));
        }

        // ── 1. Verify the Google ID token ────────────────────────────────────
        String email;
        String name;

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
            .setAudience(Collections.singletonList("YOUR_GOOGLE_CLIENT_ID"))
            .build();

            GoogleIdToken idTokenObj = verifier.verify(idToken);

            if (idTokenObj == null) {
                throw new Exception("Invalid ID token");
            }

            GoogleIdToken.Payload payload = idTokenObj.getPayload();

            email = payload.getEmail();
            name  = (String) payload.get("name");

            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Email not found in token"));
            }

            email = email.toLowerCase();
            if (name == null || name.isBlank()) {
                name = email.split("@")[0];
            }

        } catch (Exception e) {
            log.warn("Google token verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid Google token"));
        }

        // ── 2. Find or create user ────────────────────────────────────────────
        final String finalEmail = email;
        final String finalName  = name;

        User user = userRepository.findByEmail(finalEmail).orElseGet(() -> {
            // New Google user — create account
            String username = generateUsername(finalEmail);
            long userCount  = userRepository.count();

            User newUser = User.builder()
                    .name(finalName)
                    .username(username)
                    .email(finalEmail)
                    // Random password — Google users never use it
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(userCount == 0 ? "ROLE_ADMIN" : "ROLE_USER")
                    .provider("GOOGLE")
                    .createdAt(java.time.LocalDateTime.now())
                    .build();

            log.info("Creating new Google user: {}", finalEmail);
            return userRepository.save(newUser);
        });

        // ── 3. Generate JWT using existing Jwtutil  ────────────────
        String token = jwtutil.generateToken(user.getEmail());

        // ── 4. Return same shape as /api/users/login ──────────────────────────
        return ResponseEntity.ok(Map.of(
            "token",    token,
            "username", user.getRealUsername(),
            "email",    user.getEmail(),
            "name",     user.getName() != null ? user.getName() : ""
        ));
    }

    /** Derive a unique username from email prefix */
    private String generateUsername(String email) {
        String base = email.split("@")[0]
                .replaceAll("[^a-zA-Z0-9_]", "_")
                .toLowerCase();
        if (base.length() < 3) base = base + "_g";

        // Make unique if taken
        String candidate = base;
        int i = 1;
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = base + i++;
        }
        return candidate;
    }
}
