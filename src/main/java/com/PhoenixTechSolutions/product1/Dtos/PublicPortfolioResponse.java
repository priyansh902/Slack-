package com.PhoenixTechSolutions.product1.Dtos;

import java.time.LocalDateTime;
import java.util.List;

public record PublicPortfolioResponse(
    String username,
    String name,
    String email,  
    String bio,
    List<String> skills,
    String githubUrl,
    String linkedinUrl,
    List<PortfolioprojectDto> projects,
    String resumeUrl,
    int projectCount,
    LocalDateTime memberSince,
    LocalDateTime lastUpdated
) {
    
}
