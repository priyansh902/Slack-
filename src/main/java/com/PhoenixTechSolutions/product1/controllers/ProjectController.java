package com.PhoenixTechSolutions.product1.controllers;

import com.PhoenixTechSolutions.product1.Dtos.ProjectRequest;
import com.PhoenixTechSolutions.product1.Dtos.ProjectResponse;
import com.PhoenixTechSolutions.product1.model.Projects;  
import com.PhoenixTechSolutions.product1.model.User;
import com.PhoenixTechSolutions.product1.repositiory.ProjectRepository;
import com.PhoenixTechSolutions.product1.repositiory.UserRepositiory;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final UserRepositiory userRepository;

    public ProjectController(ProjectRepository projectRepository, UserRepositiory userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    //  PUBLIC/LOGGED-IN VIEW ENDPOINTS

    /**
     * Get all projects for a specific user (by user ID)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getProjectsByUserId(
            @PathVariable Long userId,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Unauthorized attempt to view projects for user ID: {}", userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User viewer = (User) authentication.getPrincipal();
        if (viewer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication"));
        }

        log.debug("User {} viewing projects for user ID: {}", viewer.getEmail(), userId);

        List<Projects> projects = projectRepository.findByUserId(userId);

        List<ProjectResponse> projectResponses = projects.stream()
                .filter(p -> p != null && p.getUser() != null)
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "count", projectResponses.size(),
            "projects", projectResponses
        ));
    }

    /**
     * Get all projects for a specific user (by username)
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getProjectsByUsername(
            @PathVariable String username,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Unauthorized attempt to view projects for username: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User viewer = (User) authentication.getPrincipal();
        if (viewer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication"));
        }

        log.debug("User {} viewing projects for username: {}", viewer.getEmail(), username);

        Optional<User> targetUserOpt = userRepository.findByUsername(username);
        
        if (targetUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found with username: " + username));
        }

        User targetUser = targetUserOpt.get();
        List<Projects> projects = projectRepository.findByUserId(targetUser.getId());

        List<ProjectResponse> projectResponses = projects.stream()
                .filter(p -> p != null && p.getUser() != null)
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "username", username,
            "userId", targetUser.getId(),
            "count", projectResponses.size(),
            "projects", projectResponses
        ));
    }

    /**
     * Get a single project by ID
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<?> getProjectById(
            @PathVariable Long projectId,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User viewer = (User) authentication.getPrincipal();
        if (viewer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication"));
        }

        Optional<Projects> projectOpt = projectRepository.findById(projectId);
        
        if (projectOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found with ID: " + projectId));
        }

        Projects project = projectOpt.get();
        
        if (project.getUser() == null) {
            log.error("Project {} has no associated user", projectId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Project data is corrupted"));
        }

        log.debug("User {} viewing project ID: {}", viewer.getEmail(), projectId);
        return ResponseEntity.ok(convertToResponse(project));
    }

    /**
     * Search projects by title
     */
    @GetMapping("/search/title")
    public ResponseEntity<?> searchByTitle(
            @RequestParam String query,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User searcher = (User) authentication.getPrincipal();
        if (searcher == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication"));
        }

        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Search query must be at least 2 characters"));
        }

        List<Projects> projects = projectRepository.findByTitleContainingIgnoreCase(query.trim());

        List<ProjectResponse> results = projects.stream()
                .filter(p -> p != null && p.getUser() != null)
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        log.info("User {} searched projects by title: '{}' - {} results", 
                searcher.getEmail(), query, results.size());

        return ResponseEntity.ok(Map.of(
            "query", query,
            "count", results.size(),
            "results", results
        ));
    }

    /**
     * Search projects by tech stack
     */
    @GetMapping("/search/tech")
    public ResponseEntity<?> searchByTechStack(
            @RequestParam String tech,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User searcher = (User) authentication.getPrincipal();
        if (searcher == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication"));
        }

        if (tech == null || tech.trim().length() < 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Search query must be at least 2 characters"));
        }

        List<Projects> projects = projectRepository.findByTechStackContaining(tech.trim());

        List<ProjectResponse> results = projects.stream()
                .filter(p -> p != null && p.getUser() != null)
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        log.info("User {} searched projects by tech: '{}' - {} results", 
                searcher.getEmail(), tech, results.size());

        return ResponseEntity.ok(Map.of(
            "tech", tech,
            "count", results.size(),
            "results", results
        ));
    }

    /**
     * Get recent projects
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentProjects(Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User viewer = (User) authentication.getPrincipal();
        if (viewer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication"));
        }

        List<Projects> recentProjects = projectRepository.findTop10ByOrderByCreatedAtDesc();

        List<ProjectResponse> results = recentProjects.stream()
                .filter(p -> p != null && p.getUser() != null)
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        log.debug("User {} viewed recent projects", viewer.getEmail());

        return ResponseEntity.ok(Map.of(
            "count", results.size(),
            "projects", results
        ));
    }

    //  OWNER-ONLY ENDPOINTS (Create/Update/Delete) ==========

    /**
     * Create a new project for the logged-in user
     */
    @PostMapping
    public ResponseEntity<?> createProject(
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User authenticatedUser = (User) authentication.getPrincipal();
        if (authenticatedUser == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "User authentication error"));
        }

        log.info("User {} creating new project: '{}'", authenticatedUser.getEmail(), request.title());

        try {
           
            User currentUser = userRepository.findById(authenticatedUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Create project
            Projects project = Projects.builder()
                    .title(request.title())
                    .description(request.description() != null ? request.description() : "")
                    .techStack(request.techStack())
                    .githubLink(request.githubLink())
                    .liveLink(request.liveLink())
                    .build();

            // Set the user on the project directly (don't use helper method)
            project.setUser(currentUser);
            
            // Save the project (this will persist the relationship)
            Projects savedProject = projectRepository.save(project);
            
            log.info("Project created successfully - ID: {}, Title: '{}', User: {}", 
                    savedProject.getId(), savedProject.getTitle(), currentUser.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(convertToResponse(savedProject));

        } catch (Exception e) {
            log.error("Failed to create project for user {}: {}", 
                    authenticatedUser.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create project: " + e.getMessage()));
        }
    }

    /**
     * Update an existing project (only if owner)
     */
    @PutMapping("/{projectId}")
    public ResponseEntity<?> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User authenticatedUser = (User) authentication.getPrincipal();
        
        Optional<Projects> projectOpt = projectRepository.findById(projectId);
        
        if (projectOpt.isEmpty()) {
            log.warn("Update failed - Project not found: ID {}", projectId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found with ID: " + projectId));
        }

        Projects project = projectOpt.get();

        // Check if user owns this project
        if (project.getUser() == null || !project.getUser().getId().equals(authenticatedUser.getId())) {
            log.warn("User {} attempted to update project belonging to another user", 
                    authenticatedUser.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only update your own projects"));
        }

        log.info("User {} updating project ID: {}", authenticatedUser.getEmail(), projectId);

        // Update fields
        if (request.title() != null && !request.title().equals(project.getTitle())) {
            project.setTitle(request.title());
        }

        if (request.description() != null) {
            project.setDescription(request.description());
        }

        if (request.techStack() != null) {
            project.setTechStack(request.techStack());
        }

        if (request.githubLink() != null) {
            project.setGithubLink(request.githubLink());
        }

        if (request.liveLink() != null) {
            project.setLiveLink(request.liveLink());
        }

        Projects updatedProject = projectRepository.save(project);

        return ResponseEntity.ok(convertToResponse(updatedProject));
    }
    /**
     * Delete a project (only if owner)
     */
    @DeleteMapping("/{projectId}")
    public ResponseEntity<?> deleteProject(
            @PathVariable Long projectId,
            Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User authenticatedUser = (User) authentication.getPrincipal();

        Optional<Projects> projectOpt = projectRepository.findById(projectId);
        
        if (projectOpt.isEmpty()) {
            log.warn("Delete failed - Project not found: ID {}", projectId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found with ID: " + projectId));
        }

        Projects project = projectOpt.get();

        // Check if user owns this project
        if (project.getUser() == null || !project.getUser().getId().equals(authenticatedUser.getId())) {
            log.warn("User {} attempted to delete project belonging to another user", 
                    authenticatedUser.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only delete your own projects"));
        }

        // Delete the project directly (don't need to remove from user's collection)
        projectRepository.delete(project);
        
        log.warn("Project deleted - ID: {}, Title: '{}', User: {}", 
                projectId, project.getTitle(), authenticatedUser.getEmail());

        return ResponseEntity.ok(Map.of(
            "message", "Project deleted successfully",
            "deletedProjectId", projectId
        ));
    }

    /**
     * Get all projects for the logged-in user (their own projects)
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyProjects(Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User currentUser = (User) authentication.getPrincipal();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "User authentication error"));
        }

        log.debug("User {} fetching their own projects", currentUser.getEmail());

        List<Projects> projects = projectRepository.findByUserId(currentUser.getId());

        List<ProjectResponse> projectResponses = projects.stream()
                .filter(p -> p != null)
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "userId", currentUser.getId(),
            "count", projectResponses.size(),
            "projects", projectResponses
        ));
    }

    // ========== ADMIN ENDPOINTS ==========

    /**
     * Get all projects (admin only)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllProjects(Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User admin = (User) authentication.getPrincipal();
        log.info("Admin {} fetching all projects", admin.getEmail());

        List<ProjectResponse> projects = projectRepository.findAll().stream()
                .filter(p -> p != null && p.getUser() != null)
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "count", projects.size(),
            "projects", projects
        ));
    }

    /**
     * Delete any project (admin only)
     */
    @DeleteMapping("/admin/{projectId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> adminDeleteProject(
            @PathVariable Long projectId,
            Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User admin = (User) authentication.getPrincipal();
        
        Optional<Projects> projectOpt = projectRepository.findById(projectId);
        
        if (projectOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found with ID: " + projectId));
        }

        Projects project = projectOpt.get();
        User projectOwner = project.getUser();
        
        // Clean up relationship if owner exists
        if (projectOwner != null) {
            projectOwner.removeProject(project);
        }
        
        projectRepository.delete(project);
        
        String ownerEmail = (projectOwner != null) ? projectOwner.getEmail() : "unknown";
        log.warn("Admin {} deleted project ID: {} belonging to user: {}", 
                admin.getEmail(), projectId, ownerEmail);

        return ResponseEntity.ok(Map.of(
            "message", "Project deleted by admin",
            "deletedProjectId", projectId
        ));
    }

    // ========== HELPER METHOD ==========

    /**
     * Convert Projects entity to ProjectResponse DTO
     */
    private ProjectResponse convertToResponse(Projects project) {
        User user = project.getUser();
        return new ProjectResponse(
            project.getId(),
            user.getId(),
            user.getUsername(),
            project.getTitle(),
            project.getDescription(),
            project.getTechStack(),
            project.getGithubLink(),
            project.getLiveLink(),
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }
}