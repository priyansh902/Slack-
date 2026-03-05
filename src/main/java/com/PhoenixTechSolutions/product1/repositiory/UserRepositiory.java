package com.PhoenixTechSolutions.product1.repositiory;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Search by username (partial match, case insensitive)
    List<User> findByUsernameContainingIgnoreCase(String username);
    
    // Search by name (partial match, case insensitive)
    List<User> findByNameContainingIgnoreCase(String name);
    
    // Search by username OR name
    List<User> findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase(String username, String name);
    
    // Get recent users
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findTopUsersByOrderByCreatedAtDesc(@Param("limit") int limit);
    
    // Custom search with native query
    @Query(value = "SELECT * FROM users WHERE LOWER(username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))", nativeQuery = true)
    List<User> searchUsersNative(@Param("searchTerm") String searchTerm);
    
}
