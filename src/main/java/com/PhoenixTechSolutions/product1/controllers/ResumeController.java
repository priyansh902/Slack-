package com.PhoenixTechSolutions.product1.controllers;

import com.PhoenixTechSolutions.product1.Dtos.ResumeResponse;
import com.PhoenixTechSolutions.product1.model.Resume;
import com.PhoenixTechSolutions.product1.model.User;
import com.PhoenixTechSolutions.product1.repositiory.ResumeRepositiory;
import com.PhoenixTechSolutions.product1.repositiory.UserRepositiory;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeRepositiory resumeRepository;
    private final UserRepositiory userRepository;

    @Value("${file.upload-dir:uploads/resumes}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public ResumeController(ResumeRepositiory resumeRepository, UserRepositiory userRepository) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
    }

    
    @Operation(summary = "Upload resume", description = "Allows the authenticated user to upload a resume file (PDF only, max 5MB). If a resume already exists, it will be replaced. Requires authentication.")

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        // Validate authentication
        if (authentication == null || authentication.getPrincipal() == null) {
            log.warn("Unauthorized attempt to upload resume");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User currentUser = (User) authentication.getPrincipal();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "User authentication error"));
        }

        log.info("User {} uploading resume", currentUser.getEmail());

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty"));
        }

        // Check file type (only PDF)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only PDF files are allowed"));
        }

        // Check file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File size exceeds 5MB limit"));
        }

        try {
            // Check if user already has a resume
            Optional<Resume> existingResume = resumeRepository.findByUserId(currentUser.getId());
            
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ? 
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".pdf";
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Save file locally
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Create file URL
            String fileUrl = baseUrl + "/api/resumes/files/" + uniqueFilename;

            Resume resume;
            boolean isUpdate = existingResume.isPresent();

            if (isUpdate) {
                // Update existing resume
                resume = existingResume.get();
                
                // Delete old file if it exists
                if (resume.getFilePath() != null) {
                    Path oldFilePath = Paths.get(resume.getFilePath());
                    Files.deleteIfExists(oldFilePath);
                }
                
                resume.setFileName(originalFilename != null ? originalFilename : "resume.pdf");
                resume.setFilePath(filePath.toString());
                resume.setFileUrl(fileUrl);
                resume.setFileSize(file.getSize());
                resume.setContentType(contentType);
                
                log.info("Updating existing resume for user: {}", currentUser.getEmail());
            } else {
                // Create new resume
                resume = Resume.builder()
                        .user(currentUser)
                        .fileName(originalFilename != null ? originalFilename : "resume.pdf")
                        .filePath(filePath.toString())
                        .fileUrl(fileUrl)
                        .fileSize(file.getSize())
                        .contentType(contentType)
                        .build();
                
                log.info("Creating new resume for user: {}", currentUser.getEmail());
            }

            Resume savedResume = resumeRepository.save(resume);
            
            log.info("Resume {} successfully for user: {}", 
                    isUpdate ? "updated" : "uploaded", currentUser.getEmail());

            return ResponseEntity.ok(Map.of(
                "message", isUpdate ? "Resume updated successfully" : "Resume uploaded successfully",
                "resume", convertToResponse(savedResume)
            ));

        } catch (IOException e) {
            log.error("Failed to upload resume for user {}: {}", currentUser.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload resume: " + e.getMessage()));
        }
    }


    @Operation(summary = "Get own resume", description = "Returns the resume details for the authenticated user. Requires authentication.")

    @GetMapping("/me")
    public ResponseEntity<?> getMyResume(Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User currentUser = (User) authentication.getPrincipal();
        log.debug("User {} fetching their resume", currentUser.getEmail());

        Optional<Resume> resumeOpt = resumeRepository.findByUserId(currentUser.getId());

        if (resumeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Resume not found. Please upload one."));
        }

        return ResponseEntity.ok(convertToResponse(resumeOpt.get()));
    }

  
    @Operation(summary = "Get resume by user ID", description = "Returns the resume details for the specified user ID. Requires authentication. Users can view their own resume or others' resumes with limited information. Admins can view all details.")

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getResumeByUserId(
            @PathVariable Long userId,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Unauthorized attempt to view resume for user ID: {}", userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User viewer = (User) authentication.getPrincipal();
        log.debug("User {} viewing resume for user ID: {}", viewer.getEmail(), userId);

        Optional<Resume> resumeOpt = resumeRepository.findByUserId(userId);

        if (resumeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Resume not found for user ID: " + userId));
        }

        Resume resume = resumeOpt.get();
        
        // Return limited info for other users (just file URL and name)
        if (!viewer.getId().equals(userId) && !viewer.isAdmin()) {
            Map<String, Object> publicView = new HashMap<>();
            publicView.put("userId", userId);
            publicView.put("fileName", resume.getFileName());
            publicView.put("fileUrl", resume.getFileUrl());
            publicView.put("uploadedAt", resume.getUploadedAt());
            return ResponseEntity.ok(publicView);
        }

        // Return full details for owner or admin
        return ResponseEntity.ok(convertToResponse(resume));
    }

    
    @Operation(summary = "Get resume by username", description = "Returns the resume details for the specified username. Requires authentication. Users can view their own resume or others' resumes with limited information. Admins can view all details.")

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getResumeByUsername(
            @PathVariable String username,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Unauthorized attempt to view resume for username: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User viewer = (User) authentication.getPrincipal();
        log.debug("User {} viewing resume for username: {}", viewer.getEmail(), username);

        Optional<User> targetUserOpt = userRepository.findByUsername(username);
        
        if (targetUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found with username: " + username));
        }

        User targetUser = targetUserOpt.get();
        Optional<Resume> resumeOpt = resumeRepository.findByUserId(targetUser.getId());

        if (resumeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Resume not found for user: " + username));
        }

        Resume resume = resumeOpt.get();
        
        // Return limited info for other users
        if (!viewer.getId().equals(targetUser.getId()) && !viewer.isAdmin()) {
            Map<String, Object> publicView = new HashMap<>();
            publicView.put("username", username);
            publicView.put("userId", targetUser.getId());
            publicView.put("fileName", resume.getFileName());
            publicView.put("fileUrl", resume.getFileUrl());
            publicView.put("uploadedAt", resume.getUploadedAt());
            return ResponseEntity.ok(publicView);
        }

        return ResponseEntity.ok(convertToResponse(resume));
    }

    
    @Operation(summary = "Delete own resume", description = "Allows the authenticated user to delete their own resume.")

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyResume(Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login required"));
        }

        User currentUser = (User) authentication.getPrincipal();

        Optional<Resume> resumeOpt = resumeRepository.findByUserId(currentUser.getId());

        if (resumeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Resume not found"));
        }

        Resume resume = resumeOpt.get();

        // Delete physical file
        try {
            Path filePath = Paths.get(resume.getFilePath());
            Files.deleteIfExists(filePath);
            log.debug("Deleted resume file: {}", resume.getFilePath());
        } catch (IOException e) {
            log.error("Failed to delete resume file: {}", e.getMessage());
            // Continue with database deletion even if file delete fails
        }

        resumeRepository.delete(resume);
        log.info("User {} deleted their resume", currentUser.getEmail());

        return ResponseEntity.ok(Map.of(
            "message", "Resume deleted successfully"
        ));
    }

    // ========== ADMIN ENDPOINTS 

    
    @Operation(summary = "Get all resumes", description = "Returns a list of all resumes in the system with user details. Requires admin privileges.")

    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllResumes(Authentication authentication) {

        User admin = (User) authentication.getPrincipal();
        log.info("Admin {} fetching all resumes", admin.getEmail());

        List<ResumeResponse> resumes = resumeRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "count", resumes.size(),
            "resumes", resumes
        ));
    }

   
    @Operation(summary = "Admin delete resume", description = "Allows an admin to delete any user's resume by resume ID. Requires admin privileges.")

    @DeleteMapping("/admin/{resumeId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> adminDeleteResume(
            @PathVariable Long resumeId,
            Authentication authentication) {

        User admin = (User) authentication.getPrincipal();

        Optional<Resume> resumeOpt = resumeRepository.findById(resumeId);

        if (resumeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Resume not found with ID: " + resumeId));
        }

        Resume resume = resumeOpt.get();
        User resumeOwner = resume.getUser();

        // Delete physical file
        try {
            Path filePath = Paths.get(resume.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete resume file: {}", e.getMessage());
        }

        resumeRepository.delete(resume);

        String ownerEmail = (resumeOwner != null) ? resumeOwner.getEmail() : "unknown";
        log.warn("Admin {} deleted resume ID: {} belonging to user: {}", 
                admin.getEmail(), resumeId, ownerEmail);

        return ResponseEntity.ok(Map.of(
            "message", "Resume deleted by admin",
            "deletedResumeId", resumeId
        ));
    }

    //  HELPER METHOD 
    @Operation(summary = "Convert resume to response format", description = "Converts a resume entity to a response DTO format for API responses.")
    private ResumeResponse convertToResponse(Resume resume) {
        User user = resume.getUser();
        return new ResumeResponse(
            resume.getId(),
            user.getId(),
            user.getRealUsername(),
            resume.getFileName(),
            resume.getFileUrl(),
            resume.getFileSize(),
            resume.getContentType(),
            resume.getUploadedAt(),
            resume.getUpdatedAt()
        );
    }
}