package com.PhoenixTechSolutions.product1.Dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank String username,
        @Email @NotBlank String email,
        @NotBlank String password
) {}