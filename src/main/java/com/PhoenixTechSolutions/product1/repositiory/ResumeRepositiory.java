package com.PhoenixTechSolutions.product1.repositiory;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import com.PhoenixTechSolutions.product1.model.Resume;

@Repository
public interface ResumeRepositiory extends JpaRepository<Resume, Long> {
     Optional <Resume> findByUserId(Long userId);

     @Query("SELECT r FROM Resume r JOIN r.user u WHERE u.username = :username")
     Optional <Resume> findByUsername(@P("username") String username);

     // Check if a resume exists for a given user ID
     boolean existsByUserId(Long userId);

     // Retrieve all resumes (Admin functionality)
     List<Resume> findAll();

     // Count the total number of resumes (Admin functionality)
     long count();

    // Delete a resume by user ID (Admin functionality & User functionality)
     void deleteByUserId(Long userId);
    
}
