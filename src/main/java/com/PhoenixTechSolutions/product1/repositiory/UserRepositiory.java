package com.PhoenixTechSolutions.product1.repositiory;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.PhoenixTechSolutions.product1.model.User;

public interface UserRepositiory extends JpaRepository<User, Long> {
   Optional<User> findByEmail(String email);
   Optional<User> findByUsername(String username);

    // For search functionality
    List<User> findByEmailContaining(String email);
    List<User> findByUsernameContaining(String username);
    
    // Check if any users exist
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    
}
