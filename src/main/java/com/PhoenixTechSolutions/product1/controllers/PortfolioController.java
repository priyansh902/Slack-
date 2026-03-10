package com.PhoenixTechSolutions.product1.controllers;

import com.PhoenixTechSolutions.product1.Dtos.PublicPortfolioResponse;
import com.PhoenixTechSolutions.product1.service.PortfolioService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

   
    @Operation(summary = "Get public portfolio by username", description = "Returns the public portfolio for the specified username. No authentication required.")

    @GetMapping("/{username}")
    public ResponseEntity<?> getPublicPortfolio(@PathVariable String username) {
        log.info("Public portfolio request for username: {}", username);

        try {
            PublicPortfolioResponse portfolio = portfolioService.getPublicPortfolio(username);
            
            // Add cache control headers 
            return ResponseEntity.ok()
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .body(portfolio);

        } catch (RuntimeException e) {
            log.warn("Portfolio not found for username: {}", username);
            
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            error.put("message", "No portfolio exists for username: " + username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            
        } catch (Exception e) {
            log.error("Error generating portfolio for {}: {}", username, e.getMessage(), e);
            
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate portfolio");
            error.put("message", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

   
    @Operation(summary = "Check if a portfolio exists for a given username", description = "Returns whether a portfolio exists for the specified username")

    @GetMapping("/exists/{username}")
    public ResponseEntity<?> checkPortfolioExists(@PathVariable String username) {
        try {
            PublicPortfolioResponse portfolio = portfolioService.getPublicPortfolio(username);
            return ResponseEntity.ok(Map.of(
                "exists", true,
                "username", username,
                "name", portfolio.name()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(Map.of(
                "exists", false,
                "username", username
            ));
        }
    }

    
    @Operation(summary = "Health check for Portfolio Service", description = "Returns the health status of the Portfolio Service")

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Portfolio Service",
            "timestamp", java.time.LocalDateTime.now()
        ));
    }
}