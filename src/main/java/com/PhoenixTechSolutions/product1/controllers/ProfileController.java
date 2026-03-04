package com.PhoenixTechSolutions.product1.controllers;

import com.PhoenixTechSolutions.product1.Dtos.ProfileRequest;
import com.PhoenixTechSolutions.product1.Dtos.ProfileResponse;
import com.PhoenixTechSolutions.product1.model.Profile;
import com.PhoenixTechSolutions.product1.model.User;
import com.PhoenixTechSolutions.product1.repositiory.ProfileRepositiory;
import com.PhoenixTechSolutions.product1.repositiory.UserRepositiory;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileRepositiory profileRepository;
    private final UserRepositiory userRepository;

    public ProfileController(ProfileRepositiory profileRepository, UserRepositiory userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create or update profile for logged-in user only
     * Each user can only modify their own profile
     */
    @PostMapping("/me")
    public ResponseEntity<?> createOrUpdateMyProfile(
            @Valid @RequestBody ProfileRequest request,
            Authentication authentication) {
        
        User currentUser = (User) authentication.getPrincipal();
        log.info("User {} creating/updating their own profile", currentUser.getEmail());

        try {
            Profile profile = profileRepository.findByUserId(currentUser.getId())
                    .orElse(Profile.builder()
                            .user(currentUser)
                            .build());

            // Update fields
            if (request.bio() != null) profile.setBio(request.bio());
            if (request.githubUrl() != null) profile.setGithubUrl(request.githubUrl());
            if (request.linkedinUrl() != null) profile.setLinkedinUrl(request.linkedinUrl());

            Profile savedProfile = profileRepository.save(profile);
            log.info("Profile updated successfully for user: {}", currentUser.getEmail());

            return ResponseEntity.ok(convertToResponse(savedProfile));

        } catch (Exception e) {
            log.error("Error updating profile for user: {}", currentUser.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save profile"));
        }
    }

    /**
     * Get logged-in user's profile only
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        log.debug("User {} fetching their own profile", currentUser.getEmail());

        Profile profile = profileRepository.findByUserId(currentUser.getId())
                .orElse(null);

        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found. Please create one."));
        }

        return ResponseEntity.ok(convertToResponse(profile));
    }

    /**
     * Get profile by user ID - Public read-only access
     * Users cannot modify profiles through this endpoint
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getProfileByUserId(@PathVariable Long userId) {
        log.debug("Public access: Fetching profile for user ID: {}", userId);

        Profile profile = profileRepository.findByUserId(userId)
                .orElse(null);

        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found for user ID: " + userId));
        }

        // Return only public information
        return ResponseEntity.ok(convertToPublicResponse(profile));
    }

    /**
     * Delete profile - Only owner or admin
     */
    @DeleteMapping("/{profileId}")
    public ResponseEntity<?> deleteProfile(
            @PathVariable Long profileId,
            Authentication authentication) {
        
        User currentUser = (User) authentication.getPrincipal();
        
        Profile profile = profileRepository.findById(profileId)
                .orElse(null);

        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found"));
        }

        //  ownership check
        if (!profile.getUser().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to delete profile belonging to user ID: {}", 
                    currentUser.getEmail(), profile.getUser().getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only delete your own profile"));
        }

        profileRepository.delete(profile);
        log.info("User {} deleted their own profile", currentUser.getEmail());

        return ResponseEntity.ok(Map.of("message", "Profile deleted successfully"));
    }

    /**
     * Admin only - Get all profiles
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllProfiles(Authentication authentication) {
        User admin = (User) authentication.getPrincipal();
        log.info("Admin {} fetching all profiles", admin.getEmail());

        var profiles = profileRepository.findAll().stream()
                .map(this::convertToResponse)
                .toList();

        return ResponseEntity.ok(profiles);
    }

    /**
     * Admin only - Delete any profile
     */
    @DeleteMapping("/admin/{profileId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> adminDeleteProfile(
            @PathVariable Long profileId,
            Authentication authentication) {
        
        User admin = (User) authentication.getPrincipal();
        
        Profile profile = profileRepository.findById(profileId)
                .orElse(null);

        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found"));
        }

        profileRepository.delete(profile);
        log.info("Admin {} deleted profile ID: {} belonging to user: {}", 
                admin.getEmail(), profileId, profile.getUser().getEmail());

        return ResponseEntity.ok(Map.of("message", "Profile deleted successfully by admin"));
    }

    // Helper methods
    private ProfileResponse convertToResponse(Profile profile) {
        User user = profile.getUser();
        return new ProfileResponse(
            profile.getId(),
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getName(),
            profile.getBio(),
            profile.getGithubUrl(),
            profile.getLinkedinUrl(),
            profile.getCreatedAt(),
            profile.getUpdatedAt()
        );
    }

    private Map<String, Object> convertToPublicResponse(Profile profile) {
        User user = profile.getUser();
        Map<String, Object> response = new HashMap<>();
        response.put("id", profile.getId());
        response.put("username", user.getUsername());
        response.put("name", user.getName());
        response.put("bio", profile.getBio());
        response.put("githubUrl", profile.getGithubUrl());
        response.put("linkedinUrl", profile.getLinkedinUrl());
        response.put("createdAt", profile.getCreatedAt());
        response.put("updatedAt", profile.getUpdatedAt());
        return response;
    }
}