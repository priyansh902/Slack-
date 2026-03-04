package com.PhoenixTechSolutions.product1.Dtos;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.Size;

public record ProfileRequest(
    
    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    String bio,
    
    @URL(message = "Invalid GitHub URL format")
    String githubUrl,
    
    @URL(message = "Invalid LinkedIn URL format")
    String linkedinUrl
) {
    
}
