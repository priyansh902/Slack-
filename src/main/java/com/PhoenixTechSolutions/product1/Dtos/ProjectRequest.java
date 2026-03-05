package com.PhoenixTechSolutions.product1.Dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record ProjectRequest(
    @NotBlank(message = "Project title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    String title,

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    String description,

    String techStack,

    @URL(message = "Invalid GitHub URL format")
    String githubLink,

    @URL(message = "Invalid live URL format")
    String liveLink
) {}