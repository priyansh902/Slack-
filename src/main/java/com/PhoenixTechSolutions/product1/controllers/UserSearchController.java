package com.PhoenixTechSolutions.product1.controllers;

import com.PhoenixTechSolutions.product1.model.Profile;
import com.PhoenixTechSolutions.product1.model.User;
import com.PhoenixTechSolutions.product1.repositiory.ProfileRepository;
import com.PhoenixTechSolutions.product1.repositiory.UserRepositiory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/search")
public class UserSearchController {

    private final UserRepositiory userRepository;
    private final ProfileRepository profileRepository;

    public UserSearchController(UserRepositiory userRepository, ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    /**
     * SEARCH USERS BY USERNAME - Partial match, case insensitive
     * Any logged-in user can search
     */
    @GetMapping("/username")
    public ResponseEntity<?> searchByUsername(
            @RequestParam String query,
            Authentication authentication) {
        
        // Check if user is logged in
        if (authentication == null) {
            log.warn("Unauthorized search attempt by username");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required to search users"));
        }

        User searcher = (User) authentication.getPrincipal();
        log.info("User {} searching users by username: '{}'", searcher.getEmail(), query);

        // Validate query length
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Search query must be at least 2 characters"));
        }

        long startTime = System.currentTimeMillis();

        try {
            List<User> users = userRepository.findByUsernameContainingIgnoreCase(query.trim());
            
            List<Map<String, Object>> results = users.stream()
                    .map(user -> {
                        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
                        return convertToSearchResult(user, profile, searcher);
                    })
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;

            log.info("Username search '{}' returned {} results in {}ms for user {}", 
                    query, results.size(), duration, searcher.getEmail());

            return ResponseEntity.ok(Map.of(
                "query", query,
                "count", results.size(),
                "results", results,
                "timeMs", duration
            ));

        } catch (Exception e) {
            log.error("Username search failed for user {}: {}", searcher.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Search failed. Please try again."));
        }
    }

    /**
     * SEARCH USERS BY NAME - Partial match, case insensitive
     * Any logged-in user can search
     */
    @GetMapping("/name")
    public ResponseEntity<?> searchByName(
            @RequestParam String query,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User searcher = (User) authentication.getPrincipal();
        log.info("User {} searching users by name: '{}'", searcher.getEmail(), query);

        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Search query must be at least 2 characters"));
        }

        long startTime = System.currentTimeMillis();

        try {
            List<User> users = userRepository.findByNameContainingIgnoreCase(query.trim());
            
            List<Map<String, Object>> results = users.stream()
                    .map(user -> {
                        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
                        return convertToSearchResult(user, profile, searcher);
                    })
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;

            log.info("Name search '{}' returned {} results in {}ms for user {}", 
                    query, results.size(), duration, searcher.getEmail());

            return ResponseEntity.ok(Map.of(
                "query", query,
                "count", results.size(),
                "results", results,
                "timeMs", duration
            ));

        } catch (Exception e) {
            log.error("Name search failed for user {}: {}", searcher.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Search failed"));
        }
    }

    /**
     * SEARCH USERS BY KEYWORD - Searches both username and name
     * Any logged-in user can search
     */
    @GetMapping("/keyword")
    public ResponseEntity<?> searchByKeyword(
            @RequestParam String keyword,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User searcher = (User) authentication.getPrincipal();
        log.info("User {} searching users with keyword: '{}'", searcher.getEmail(), keyword);

        if (keyword == null || keyword.trim().length() < 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Search keyword must be at least 2 characters"));
        }

        long startTime = System.currentTimeMillis();

        try {
            String searchTerm = keyword.trim();
            List<User> users = userRepository.findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase(
                    searchTerm, searchTerm);
            
            List<Map<String, Object>> results = users.stream()
                    .map(user -> {
                        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
                        return convertToSearchResult(user, profile, searcher);
                    })
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;

            log.info("Keyword search '{}' returned {} results in {}ms for user {}", 
                    keyword, results.size(), duration, searcher.getEmail());

            return ResponseEntity.ok(Map.of(
                "keyword", keyword,
                "count", results.size(),
                "results", results,
                "timeMs", duration
            ));

        } catch (Exception e) {
            log.error("Keyword search failed for user {}: {}", searcher.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Search failed"));
        }
    }

    /**
     * GET USER BY ID - Direct lookup
     */
    @GetMapping("/user/id/{userId}")
    public ResponseEntity<?> getUserById(
            @PathVariable Long userId,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User searcher = (User) authentication.getPrincipal();
        log.info("User {} looking up user by ID: {}", searcher.getEmail(), userId);

        try {
            User targetUser = userRepository.findById(userId)
                    .orElse(null);

            if (targetUser == null) {
                log.warn("User not found with ID: {} - Requested by: {}", userId, searcher.getEmail());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found with ID: " + userId));
            }

            Profile profile = profileRepository.findByUserId(userId).orElse(null);
            
            log.info("User {} found user by ID: {} - Username: {}", 
                    searcher.getEmail(), userId, targetUser.getUsername());
            
            return ResponseEntity.ok(convertToSearchResult(targetUser, profile, searcher));

        } catch (Exception e) {
            log.error("User lookup by ID failed for user {}: {}", searcher.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch user"));
        }
    }

    /**
     * GET USER BY EXACT USERNAME - Direct lookup
     */
    @GetMapping("/user/username/{username}")
    public ResponseEntity<?> getUserByExactUsername(
            @PathVariable String username,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User searcher = (User) authentication.getPrincipal();
        log.info("User {} looking up user by exact username: '{}'", searcher.getEmail(), username);

        try {
            User targetUser = userRepository.findByUsername(username)
                    .orElse(null);

            if (targetUser == null) {
                log.warn("User not found with username: '{}' - Requested by: {}", 
                        username, searcher.getEmail());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found with username: " + username));
            }

            Profile profile = profileRepository.findByUserId(targetUser.getId()).orElse(null);
            
            log.info("User {} found user by username: '{}' - ID: {}", 
                    searcher.getEmail(), username, targetUser.getId());
            
            return ResponseEntity.ok(convertToSearchResult(targetUser, profile, searcher));

        } catch (Exception e) {
            log.error("User lookup by username failed for user {}: {}", searcher.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch user"));
        }
    }

    /**
     * AUTOCOMPLETE SUGGESTIONS - For search bars
     * Returns limited fields for fast response
     */
    @GetMapping("/suggestions")
    public ResponseEntity<?> getSearchSuggestions(
            @RequestParam String query,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User searcher = (User) authentication.getPrincipal();

        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(Map.of(
                "query", query,
                "suggestions", List.of()
            ));
        }

        long startTime = System.currentTimeMillis();

        try {
            String searchTerm = query.trim();
            List<Map<String, Object>> suggestions = userRepository
                    .findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase(searchTerm, searchTerm)
                    .stream()
                    .limit(10)  // Limit for performance
                    .map(user -> {
                        Map<String, Object> suggestion = new HashMap<>();
                        suggestion.put("userId", user.getId());
                        suggestion.put("username", user.getUsername());
                        suggestion.put("name", user.getName());
                        suggestion.put("hasProfile", profileRepository.existsByUserId(user.getId()));
                        return suggestion;
                    })
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;

            log.debug("Suggestions for '{}' returned {} results in {}ms for user {}", 
                    query, suggestions.size(), duration, searcher.getEmail());

            return ResponseEntity.ok(Map.of(
                "query", query,
                "suggestions", suggestions,
                "count", suggestions.size(),
                "timeMs", duration
            ));

        } catch (Exception e) {
            log.error("Suggestions failed for user {}: {}", searcher.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get suggestions"));
        }
    }

    /**
     * RECENT USERS - Get recently joined users
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentUsers(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User searcher = (User) authentication.getPrincipal();
        log.debug("User {} requesting recent users (limit: {})", searcher.getEmail(), limit);

        try {
            // Limit to reasonable number
            int resultLimit = Math.min(limit, 50);
            
            List<User> recentUsers = userRepository.findTopUsersByOrderByCreatedAtDesc(resultLimit);
            
            List<Map<String, Object>> results = recentUsers.stream()
                    .map(user -> {
                        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
                        return convertToSearchResult(user, profile, searcher);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "count", results.size(),
                "users", results
            ));

        } catch (Exception e) {
            log.error("Failed to fetch recent users for {}: {}", searcher.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch recent users"));
        }
    }

    // HELPER METHODS 

    /**
     * Convert user to search result format
     */
    private Map<String, Object> convertToSearchResult(User user, Profile profile, User searcher) {
        Map<String, Object> result = new HashMap<>();
        
        // Basic user info
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("name", user.getName());
        result.put("memberSince", user.getCreatedAt());
        
        // Profile info if exists
        if (profile != null) {
            result.put("profileId", profile.getId());
            result.put("bio", truncate(profile.getBio(), 100));
            result.put("githubUrl", profile.getGithubUrl());
            result.put("linkedinUrl", profile.getLinkedinUrl());
            result.put("hasProfile", true);
        } else {
            result.put("hasProfile", false);
        }
        
        // Indicate if this is the searcher's own profile
        result.put("isYou", searcher.getId().equals(user.getId()));
        
        return result;
    }

    /**
     * Truncate long text
     */
    private String truncate(String str, int length) {
        if (str == null || str.length() <= length) return str;
        return str.substring(0, length) + "...";
    }
}