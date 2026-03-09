package com.PhoenixTechSolutions.product1.Dtos;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

public record ResumeRequet(
    @NotNull(
        message = "Resume file must not be null"
    )
    MultipartFile file
) {
    
}
