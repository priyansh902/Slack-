package com.PhoenixTechSolutions.product1.Dtos;

import java.time.LocalDateTime;

public record ProfileResponse(

    Long id,
    Long userId,
    String username,
    String email,
    String name,
    String bio,
    String skills,
    String githubUrl,
    String linkedinUrl,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
    
) {
    
}
