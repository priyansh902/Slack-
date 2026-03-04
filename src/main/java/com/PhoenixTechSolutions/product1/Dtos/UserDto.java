package com.PhoenixTechSolutions.product1.Dtos;

import java.time.LocalDateTime;

public record UserDto(
    Long id,
    String name,
    String email,
    String username,
    String role,
    LocalDateTime createdAt
) {


    
}
