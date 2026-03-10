package com.PhoenixTechSolutions.product1.Dtos;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.Size;

public record ProfileRequest(
    
    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    String bio,

    @Size(max = 500, message = "Skills cannot exceed 500 characters")
    String skills,
    
    @URL(message = "Invalid GitHub URL format")
    String githubUrl,
    
    @URL(message = "Invalid LinkedIn URL format, must start with http:// or https://" )
    String linkedinUrl
) {
    
}
