package com.PhoenixTechSolutions.product1.repositiory;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.PhoenixTechSolutions.product1.model.User;

public interface UserRepositiory extends JpaRepository<User, Long> {
   Optional<User> findByEmail(String email);
    
}
