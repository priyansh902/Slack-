package com.PhoenixTechSolutions.product1.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.PhoenixTechSolutions.product1.model.Projects;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Projects, Long> {

    // Find all projects by user ID
    List<Projects> findByUserId(Long userId);

    // Find all projects by username
    @Query("SELECT p FROM Projects p WHERE p.user.username = :username")
    List<Projects> findByUsername(@Param("username") String username);

    // Search projects by title (partial match, case insensitive)
    List<Projects> findByTitleContainingIgnoreCase(String title);

    // Search projects by tech stack
    @Query("SELECT p FROM Projects p WHERE LOWER(p.techStack) LIKE LOWER(CONCAT('%', :tech, '%'))")
    List<Projects> findByTechStackContaining(@Param("tech") String tech);

    // Find recent projects
    List<Projects> findTop10ByOrderByCreatedAtDesc();

    // Find projects by user and search in title/description
    @Query("SELECT p FROM Projects p WHERE p.user.id = :userId AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Projects> searchUserProjects(@Param("userId") Long userId, @Param("search") String search);

    // Count projects by user
    Long countByUserId(Long userId);

    // Check if user owns the project
    boolean existsByIdAndUserId(Long id, Long userId);

    // Delete all projects by user ID
    void deleteByUserId(Long userId);
}