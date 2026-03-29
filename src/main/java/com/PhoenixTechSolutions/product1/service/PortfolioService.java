package com.PhoenixTechSolutions.product1.service;

import com.PhoenixTechSolutions.product1.Dtos.PortfolioprojectDto;
import com.PhoenixTechSolutions.product1.Dtos.PublicPortfolioResponse;
import com.PhoenixTechSolutions.product1.model.*;
import com.PhoenixTechSolutions.product1.repositiory.ProfileRepository;
import com.PhoenixTechSolutions.product1.repositiory.ProjectRepository;
import com.PhoenixTechSolutions.product1.repositiory.ResumeRepositiory;
import com.PhoenixTechSolutions.product1.repositiory.UserRepositiory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PortfolioService {

    private final UserRepositiory userRepository;
    private final ProfileRepository profileRepository;
    private final ProjectRepository projectRepository;
    private final ResumeRepositiory resumeRepository;

    public PortfolioService(
            UserRepositiory userRepository,
            ProfileRepository profileRepository,
            ProjectRepository projectRepository,
            ResumeRepositiory resumeRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.projectRepository = projectRepository;
        this.resumeRepository = resumeRepository;
    }

    /**
     * Generate public portfolio for a username
     */
    public PublicPortfolioResponse getPublicPortfolio(String username) {
        log.info("Generating public portfolio for username: {}", username);

        //  Find the user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        //  Get profile data
        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        
        String bio = (profile != null && profile.getBio() != null) ? profile.getBio() : "";
        String githubUrl = (profile != null) ? profile.getGithubUrl() : null;
        String linkedinUrl = (profile != null) ? profile.getLinkedinUrl() : null;
        
        // Parse skills (comma-separated) into list
        List<String> skills = Collections.emptyList();
        if (profile != null && profile.getSkills() != null && !profile.getSkills().isEmpty()) {
            skills = Arrays.stream(profile.getSkills().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        // 3. Get user's projects
        List<Projects> projects = projectRepository.findByUserId(user.getId());
        List<PortfolioprojectDto> projectDtos = projects.stream()
                .map(p -> new PortfolioprojectDto(
                        p.getId(),
                        p.getTitle(),
                        p.getDescription() != null ? p.getDescription() : "",
                        p.getTechStack() != null ? p.getTechStack() : "",
                        p.getGithubLink(),
                        p.getLiveLink()))
                .collect(Collectors.toList());

        //  Get resume download URL
        Resume resume = resumeRepository.findByUserId(user.getId()).orElse(null);
        String resumeUrl = (resume != null) ? resume.getFileUrl() : null;

        //  Get last updated time (latest of profile, projects, or resume)
        LocalDateTime lastUpdated = findLastUpdated(profile, projects, resume);

        log.info("Portfolio generated for user: {} with {} projects", username, projectDtos.size());

        return new PublicPortfolioResponse(
                user.getRealUsername(),
                user.getName(),
                user.getEmail(), 
                bio,
                skills,
                githubUrl,
                linkedinUrl,
                projectDtos,
                resumeUrl,
                projectDtos.size(),
                user.getCreatedAt(),
                lastUpdated
        );
    }

    /**
     * Find the most recent update time across all user data
     */
    private LocalDateTime findLastUpdated(Profile profile, List<Projects> projects, Resume resume) {
        LocalDateTime latest = null;
        
        if (profile != null && profile.getUpdatedAt() != null) {
            latest = profile.getUpdatedAt();
        }
        
        if (resume != null && resume.getUpdatedAt() != null) {
            if (latest == null || resume.getUpdatedAt().isAfter(latest)) {
                latest = resume.getUpdatedAt();
            }
        }
        
        for (Projects project : projects) {
            if (project.getUpdatedAt() != null) {
                if (latest == null || project.getUpdatedAt().isAfter(latest)) {
                    latest = project.getUpdatedAt();
                }
            }
        }
        
        return latest != null ? latest : (profile != null ? profile.getCreatedAt() : null);
    }
}