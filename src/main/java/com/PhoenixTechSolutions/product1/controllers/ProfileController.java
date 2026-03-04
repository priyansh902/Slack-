package com.PhoenixTechSolutions.product1.controllers;

import com.PhoenixTechSolutions.product1.Dtos.ProfileRequest;
import com.PhoenixTechSolutions.product1.Dtos.ProfileResponse;
import com.PhoenixTechSolutions.product1.model.Profile;
import com.PhoenixTechSolutions.product1.model.User;
import com.PhoenixTechSolutions.product1.repositiory.ProfileRepository;
import com.PhoenixTechSolutions.product1.repositiory.UserRepositiory;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * VIEW ANY PROFILE - Requires login
     * Any authenticated user can view any profile
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getProfileByUserId(
            @PathVariable Long userId,
            Authentication authentication) {
        
        // Check if user is logged in
        if (authentication == null) {
            log.warn("Unauthorized attempt to view profile ID: {}", userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required to view profiles"));
        }

        User viewer = (User) authentication.getPrincipal();
        log.debug("User {} viewing profile for user ID: {}", viewer.getEmail(), userId);

        Profile profile = profileRepository.findByUserId(userId)
                .orElse(null);

        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found"));
        }

        // Return profile view (email hidden for others, visible for owner)
        return ResponseEntity.ok(convertToProfileView(profile, viewer));
    }

    /**
     * VIEW PROFILE BY USERNAME - Requires login
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getProfileByUsername(
            @PathVariable String username,
            Authentication authentication) {
        
        // Check if user is logged in
        if (authentication == null) {
            log.warn("Unauthorized attempt to view profile for username: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required to view profiles"));
        }

        User viewer = (User) authentication.getPrincipal();
        log.debug("User {} viewing profile for username: {}", viewer.getEmail(), username);

        User targetUser = userRepository.findByUsername(username)
                .orElse(null);

        if (targetUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }

        Profile profile = profileRepository.findByUserId(targetUser.getId())
                .orElse(null);

        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found"));
        }

        return ResponseEntity.ok(convertToProfileView(profile, viewer));
    }

    /**
     * LIST ALL PROFILES - Requires login
     * Authenticated users can browse all profiles
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllProfiles(Authentication authentication) {
        // Check if user is logged in
        if (authentication == null) {
            log.warn("Unauthorized attempt to view all profiles");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required to view profiles"));
        }

        User viewer = (User) authentication.getPrincipal();
        log.debug("User {} viewing all profiles", viewer.getEmail());
        
        List<Map<String, Object>> profiles = profileRepository.findAll().stream()
                .map(profile -> convertToProfileView(profile, viewer))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(profiles);
    }

    /**
     * GET OWN PROFILE - Full details
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        log.debug("User {} viewing their own profile", currentUser.getEmail());

        Profile profile = profileRepository.findByUserId(currentUser.getId())
                .orElse(null);

        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found. Please create one."));
        }

        // Return full profile (includes email for the owner)
        return ResponseEntity.ok(convertToFullView(profile));
    }

    /**
     * CREATE/UPDATE OWN PROFILE
     */
    @PostMapping("/me")
    public ResponseEntity<?> createOrUpdateMyProfile(
            @Valid @RequestBody ProfileRequest request,
            Authentication authentication) {
        
        User currentUser = (User) authentication.getPrincipal();
        log.info("User {} updating their profile", currentUser.getEmail());

        Profile profile = profileRepository.findByUserId(currentUser.getId())
                .orElse(Profile.builder()
                        .user(currentUser)
                        .build());

        // Update fields
        if (request.bio() != null) profile.setBio(request.bio());
        if (request.githubUrl() != null) profile.setGithubUrl(request.githubUrl());
        if (request.linkedinUrl() != null) profile.setLinkedinUrl(request.linkedinUrl());

        Profile savedProfile = profileRepository.save(profile);
        log.info("Profile updated for user: {}", currentUser.getEmail());

        return ResponseEntity.ok(convertToFullView(savedProfile));
    }

    /**
     * DELETE OWN PROFILE
     */
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyProfile(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        
        Profile profile = profileRepository.findByUserId(currentUser.getId())
                .orElse(null);

        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found"));
        }

        profileRepository.delete(profile);
        log.info("User {} deleted their profile", currentUser.getEmail());

        return ResponseEntity.ok(Map.of("message", "Profile deleted successfully"));
    }

    /**
     * ADMIN ONLY - Get all profiles with full details
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ProfileResponse>> getAllProfilesForAdmin(Authentication authentication) {
        User admin = (User) authentication.getPrincipal();
        log.info("Admin {} fetching all profiles", admin.getEmail());

        List<ProfileResponse> profiles = profileRepository.findAll().stream()
                .map(this::convertToFullView)
                .collect(Collectors.toList());

        return ResponseEntity.ok(profiles);
    }

    /**
     * ADMIN ONLY - Delete any profile
     */
    @DeleteMapping("/admin/{profileId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> adminDeleteProfile(@PathVariable Long profileId, Authentication authentication) {
        User admin = (User) authentication.getPrincipal();
        
        Profile profile = profileRepository.findById(profileId)
                .orElse(null);

        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found"));
        }

        profileRepository.delete(profile);
        log.info("Admin {} deleted profile ID: {}", admin.getEmail(), profileId);

        return ResponseEntity.ok(Map.of("message", "Profile deleted by admin"));
    }

    /**
     * PROFILE VIEW - For viewing other users' profiles
     * Email is hidden unless viewer is the owner
     */
    private Map<String, Object> convertToProfileView(Profile profile, User viewer) {
        User profileOwner = profile.getUser();
        Map<String, Object> view = new HashMap<>();
        
        // Basic info always visible
        view.put("id", profile.getId());
        view.put("userId", profileOwner.getId());
        view.put("username", profileOwner.getUsername());
        view.put("name", profileOwner.getName());
        view.put("bio", profile.getBio());
        view.put("githubUrl", profile.getGithubUrl());
        view.put("linkedinUrl", profile.getLinkedinUrl());
        view.put("memberSince", profileOwner.getCreatedAt());
        view.put("profileUpdated", profile.getUpdatedAt());
        
        // Email only visible to the profile owner
        if (viewer.getId().equals(profileOwner.getId())) {
            view.put("email", profileOwner.getEmail());
        }
        
        return view;
    }

    /**
     * FULL VIEW - For owner and admin
     */
    private ProfileResponse convertToFullView(Profile profile) {
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
            user.getCreatedAt(),
            profile.getUpdatedAt()
        );
    }
}