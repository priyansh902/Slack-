package com.PhoenixTechSolutions.product1.controllers;

import com.PhoenixTechSolutions.product1.Dtos.ProfileRequest;
import com.PhoenixTechSolutions.product1.Dtos.ProfileResponse;
import com.PhoenixTechSolutions.product1.model.Profile;
import com.PhoenixTechSolutions.product1.model.User;
import com.PhoenixTechSolutions.product1.repositiory.ProfileRepository;
import com.PhoenixTechSolutions.product1.repositiory.UserRepositiory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileRepository profileRepository;
    private final UserRepositiory userRepository;

    public ProfileController(ProfileRepository profileRepository, UserRepositiory userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    
    @Operation(summary = "View a user's profile by user ID", description = "Returns the profile information for the specified user ID. Requires login to view profiles.")

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getProfileByUserId(
            @PathVariable Long userId,
            Authentication authentication) {
        
        // Check authentication
        if (authentication == null) {
            log.warn("Unauthorized attempt to view profile ID: {}", userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required to view profiles"));
        }

        User viewer = (User) authentication.getPrincipal();
        if (viewer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication"));
        }

        log.debug("User {} viewing profile for user ID: {}", viewer.getEmail(), userId);

        // Find profile safely
        Optional<Profile> profileOpt = profileRepository.findByUserId(userId);
        
        if (profileOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found for user ID: " + userId));
        }

        Profile profile = profileOpt.get();
        
        // Validate profile has user
        if (profile.getUser() == null) {
            log.error("Profile {} has no associated user", profile.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Profile data is corrupted"));
        }

        return ResponseEntity.ok(convertToProfileView(profile, viewer));
    }

    
    @Operation(summary = "View a user's profile by username", description = "Returns the profile information for the specified username. Requires login to view profiles.")

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getProfileByUsername(
            @PathVariable String username,
            Authentication authentication) {
        
        if (authentication == null) {
            log.warn("Unauthorized attempt to view profile for username: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required to view profiles"));
        }

        User viewer = (User) authentication.getPrincipal();
        if (viewer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication"));
        }

        log.debug("User {} viewing profile for username: {}", viewer.getEmail(), username);

        // Find user by username
        Optional<User> targetUserOpt = userRepository.findByUsername(username);
        
        if (targetUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found with username: " + username));
        }

        User targetUser = targetUserOpt.get();
        
        // Find profile by user ID
        Optional<Profile> profileOpt = profileRepository.findByUserId(targetUser.getId());
        
        if (profileOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found for user: " + username));
        }

        Profile profile = profileOpt.get();
        
        if (profile.getUser() == null) {
            log.error("Profile {} has no associated user", profile.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Profile data is corrupted"));
        }

        return ResponseEntity.ok(convertToProfileView(profile, viewer));
    }

    
    @Operation(summary = "List all profiles", description = "Returns a list of all user profiles. Requires login to view profiles.")

    @GetMapping("/all")
    public ResponseEntity<?> getAllProfiles(Authentication authentication) {
        if (authentication == null) {
            log.warn("Unauthorized attempt to view all profiles");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required to view profiles"));
        }

        User viewer = (User) authentication.getPrincipal();
        if (viewer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication"));
        }

        log.debug("User {} viewing all profiles", viewer.getEmail());
        
        List<Map<String, Object>> profiles = profileRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getUser() != null)  // Only include profiles with valid users
                .map(profile -> convertToProfileView(profile, viewer))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
            "count", profiles.size(),
            "profiles", profiles
        ));
    }

    
    @Operation(summary = "Get own profile", description = "Returns the authenticated user's own profile information. Requires login.")

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User currentUser = (User) authentication.getPrincipal();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication"));
        }

        log.debug("User {} viewing their own profile", currentUser.getEmail());

    
        Profile profile = null;
        try {
            profile = currentUser.getProfile();
        } catch (Exception e) {
            log.debug("Error accessing profile: {}", e.getMessage());
        }

        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found. Please create one."));
        }

        return ResponseEntity.ok(convertToFullView(profile));
    }

   
    
    @Operation(summary = "Create or update own profile", description = "Allows the authenticated user to create a new profile or update their existing profile. Requires login.")

    @PostMapping("/me")
    public ResponseEntity<?> createOrUpdateMyProfile(
            @Valid @RequestBody ProfileRequest request,
            Authentication authentication) {
        
        // Validate authentication
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User currentUser = (User) authentication.getPrincipal();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "User authentication error"));
        }

        log.info("User {} updating their profile", currentUser.getEmail());

        try {
            // Use Optional to handle null safely
            Optional<Profile> existingProfileOpt = Optional.ofNullable(currentUser.getProfile());
            
            Profile profile;
            boolean isNew;
            
            if (existingProfileOpt.isEmpty()) {
                // Create new profile
                isNew = true;
                profile = Profile.builder()
                        .bio(Optional.ofNullable(request.bio()).orElse(""))
                        .skills(Optional.ofNullable(request.skills()).orElse(""))
                        .githubUrl(request.githubUrl())
                        .linkedinUrl(request.linkedinUrl())
                        .build();
                
                currentUser.setProfile(profile);
                log.debug("Creating new profile for user: {}", currentUser.getEmail());
                
            } else {
                // Update existing profile
                isNew = false;
                profile = existingProfileOpt.get();  
                
                Optional.ofNullable(request.bio()).ifPresent(profile::setBio);
                Optional.ofNullable(request.skills()).ifPresent(profile::setSkills);
                Optional.ofNullable(request.githubUrl()).ifPresent(profile::setGithubUrl);
                Optional.ofNullable(request.linkedinUrl()).ifPresent(profile::setLinkedinUrl);
                
                log.debug("Updating existing profile ID: {} for user: {}", 
                        profile.getId(), currentUser.getEmail());
            }

            // Save and return
            Profile savedProfile = profileRepository.save(profile);
            
            return ResponseEntity.ok(convertToFullView(savedProfile));

        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update profile"));
        }
    }

    
    @Operation(summary = "Delete own profile", description = "Allows the authenticated user to delete their own profile. Requires login.")

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyProfile(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User currentUser = (User) authentication.getPrincipal();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication"));
        }

        // Check if user has a profile safely
        boolean hasProfile = false;
        try {
            hasProfile = currentUser.hasProfile();
        } catch (Exception e) {
            log.debug("Error checking profile: {}", e.getMessage());
        }
        
        if (!hasProfile) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found"));
        }

        Profile profile = null;
        try {
            profile = currentUser.getProfile();
        } catch (Exception e) {
            log.error("Error getting profile: {}", e.getMessage());
        }
        
        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found"));
        }

        // Use helper method to clean up relationship
        try {
            currentUser.removeProfile();
        } catch (Exception e) {
            log.error("Error removing profile relationship: {}", e.getMessage());
        }
        
        // Delete the profile
        try {
            profileRepository.delete(profile);
        } catch (Exception e) {
            log.error("Error deleting profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete profile"));
        }
        
        log.info("User {} deleted their profile", currentUser.getEmail());
        
        return ResponseEntity.ok(Map.of(
            "message", "Profile deleted successfully",
            "deletedProfileId", profile.getId()
        ));
    }

    
    @Operation(summary = "Get all profiles for admin", description = "Returns a list of all user profiles with full details. Requires admin privileges.")

    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllProfilesForAdmin(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User admin = (User) authentication.getPrincipal();
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication"));
        }

        log.info("Admin {} fetching all profiles", admin.getEmail());

        List<ProfileResponse> profiles = profileRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getUser() != null)
                .map(this::convertToFullView)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "count", profiles.size(),
            "profiles", profiles
        ));
    }

    
    @Operation(summary = "Admin delete profile", description = "Allows an admin to delete any user's profile by profile ID. Requires admin privileges.")

    @DeleteMapping("/admin/{profileId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> adminDeleteProfile(
            @PathVariable Long profileId, 
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User admin = (User) authentication.getPrincipal();
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication"));
        }
        
        
        Optional<Profile> profileOpt = profileRepository.findById(profileId);
        
        if (profileOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found with ID: " + profileId));
        }

        Profile profile = profileOpt.get();
        User profileOwner = profile.getUser();
        
        // Clean up relationship if owner exists
        if (profileOwner != null) {
            try {
                profileOwner.removeProfile();
            } catch (Exception e) {
                log.error("Error removing profile relationship: {}", e.getMessage());
            }
        }
        
        // Delete the profile
        try {
            profileRepository.delete(profile);
        } catch (Exception e) {
            log.error("Error deleting profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete profile"));
        }
        
        String ownerEmail = (profileOwner != null) ? profileOwner.getEmail() : "unknown";
        log.warn("Admin {} deleted profile ID: {} belonging to user: {}", 
                admin.getEmail(), profileId, ownerEmail);

        return ResponseEntity.ok(Map.of(
            "message", "Profile deleted by admin",
            "deletedProfileId", profileId
        ));
    }

    // HELPER METHODS 

    @Operation(summary = "Convert profile to view format", description = "Converts a profile entity to a view DTO format for API responses.")
    private Map<String, Object> convertToProfileView(Profile profile, User viewer) {
        // This method is only called after profile and profile.getUser() are validated
        User profileOwner = profile.getUser();
        
        Map<String, Object> view = new HashMap<>();
        
        // Profile info
        view.put("id", profile.getId());
        view.put("bio", profile.getBio() != null ? profile.getBio() : "");

        // Parse skills into list for frontend
        List<String> skillsList = new ArrayList<>();
        if (profile.getSkills() != null && !profile.getSkills().isEmpty()) {
            skillsList = Arrays.stream(profile.getSkills().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        view.put("skills", skillsList);
        view.put("skillsRaw", profile.getSkills());

        view.put("githubUrl", profile.getGithubUrl());
        view.put("linkedinUrl", profile.getLinkedinUrl());
        view.put("profileUpdated", profile.getUpdatedAt());
        
        // User info
        view.put("userId", profileOwner.getId());
        view.put("username", profileOwner.getUsername());
        view.put("name", profileOwner.getName());
        view.put("memberSince", profileOwner.getCreatedAt());
        
        // Project info
        try {
            view.put("hasProjects", profileOwner.hasProjects());
            view.put("projectCount", profileOwner.getProjectCount());
        } catch (Exception e) {
            log.debug("Error getting project info: {}", e.getMessage());
            view.put("hasProjects", false);
            view.put("projectCount", 0);
        }
        
        // Email only visible to the profile owner or admin
        if (viewer != null && (viewer.getId().equals(profileOwner.getId()) || 
                               "ROLE_ADMIN".equals(viewer.getRole()))) {
            view.put("email", profileOwner.getEmail());
        }
        
        return view;
    }

    @Operation(summary = "Convert profile to full view format", description = "Converts a profile entity to a full view DTO format for API responses.")
    private ProfileResponse convertToFullView(Profile profile) {
        // This method is only called after profile and profile.getUser() are validated
        User user = profile.getUser();
        
        return new ProfileResponse(
            profile.getId(),
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getName(),
            profile.getBio() != null ? profile.getBio() : "",
            profile.getSkills() != null ? profile.getSkills() : "",
            profile.getGithubUrl(),
            profile.getLinkedinUrl(),
            user.getCreatedAt(),
            profile.getUpdatedAt()
        );
    }
}