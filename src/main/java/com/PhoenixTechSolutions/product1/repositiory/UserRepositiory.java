package com.PhoenixTechSolutions.product1.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;

import com.PhoenixTechSolutions.product1.model.User;

public interface UserRepositiory extends JpaRepository<User, Long> {
    User findByEmail(String email);
    
}
