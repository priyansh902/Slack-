package com.PhoenixTechSolutions.product1.Dtos;

public record LoginResponse(
    String token,
    String username,
    String name
) {
    
}
