package com.PhoenixTechSolutions.product1.Dtos;

import java.time.LocalDateTime;

public record ResumeResponse(
    Long id,
    Long userId,
    String username,
    String fileName,
    String fileUrl,
    Long fileSize,
    String contentType,
    LocalDateTime uploadedAt,
    LocalDateTime updatedAt
) {
    
}
