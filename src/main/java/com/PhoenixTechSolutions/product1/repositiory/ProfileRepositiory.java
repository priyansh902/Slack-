package com.PhoenixTechSolutions.product1.repositiory;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.PhoenixTechSolutions.product1.model.Profile;

@Repository
public interface ProfileRepositiory extends JpaRepository<Profile , Long > {

    Optional<Profile> findByUserId(Long userId);
    Optional<Profile> findByUserEmail(String email);
    boolean existsByUserId(Long userId);
    
}
