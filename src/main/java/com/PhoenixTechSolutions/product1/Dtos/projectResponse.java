package com.PhoenixTechSolutions.product1.Dtos;

import java.time.LocalDateTime;

public record ProjectResponse(
    Long id,
    Long userId,
    String username,
    String title,
    String description,
    String techStack,
    String githubLink,
    String liveLink,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}