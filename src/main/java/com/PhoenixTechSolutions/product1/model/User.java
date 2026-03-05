package com.PhoenixTechSolutions.product1.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "ROLE_USER";

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Profile profile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Projects> projects = new ArrayList<>();

    //  HELPER METHODS FOR PROFILE ==========

    /**
     * Helper method to set profile maintaining bidirectional relationship
     */
    public void setProfile(Profile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setUser(this);
        }
    }

    /**
     * Helper method to remove profile
     */
    public void removeProfile() {
        if (this.profile != null) {
            this.profile.setUser(null);
            this.profile = null;
        }
    }

    /**
     * Check if user has a profile
     */
    public boolean hasProfile() {
        return this.profile != null;
    }

    // ========== HELPER METHODS FOR PROJECTS 

    /**
     * Helper method to add a project maintaining bidirectional relationship
     * FIXED: Parameter name conflict resolved
     */
    public void addProject(Projects project) { 
        if (project == null) {                
            return;
        }
        
        if (this.projects == null) {
            this.projects = new ArrayList<>();
        }
        
        this.projects.add(project);              
        project.setUser(this);             // Set the user reference in the project to maintain bidirectional relationship      
    }

    /**
     * Helper method to add multiple projects
     */
    public void addProjects(List<Projects> projects) {
        if (projects == null) {
            return;
        }
        
        for (Projects project : projects) {
            addProject(project);                  
        }
    }

    /**
     * Helper method to remove a project maintaining bidirectional relationship
     */
    public void removeProject(Projects project) {
        if (project == null || this.projects == null) {
            return;
        }
        
        this.projects.remove(project);
        project.setUser(null);
    }

    /**
     * Helper method to remove a project by ID
     */
    public boolean removeProjectById(Long projectId) {
        if (this.projects == null || projectId == null) {
            return false;
        }
        
        return this.projects.removeIf(project -> {
            if (project.getId() != null && project.getId().equals(projectId)) {
                project.setUser(null);
                return true;
            }
            return false;
        });
    }

    /**
     * Helper method to remove all projects
     */
    public void removeAllProjects() {
        if (this.projects != null) {
            for (Projects project : new ArrayList<>(this.projects)) {
                removeProject(project);
            }
        }
    }

    /**
     * Get project by ID (if it belongs to this user)
     */
    public Projects getProjectById(Long projectId) {
        if (projects == null || projectId == null) {
            return null;
        }
        
        return projects.stream()
                .filter(p -> p.getId() != null && p.getId().equals(projectId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get total number of projects
     */
    public int getProjectCount() {
        return projects != null ? projects.size() : 0;
    }

    /**
     * Check if user has any projects
     */
    public boolean hasProjects() {
        return projects != null && !projects.isEmpty();
    }

    //  OTHER UTILITY METHODS 

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return "ROLE_ADMIN".equals(this.role);
    }

    /**
     * Get user's display name
     */
    public String getDisplayName() {
        return name != null && !name.isEmpty() ? name : username;
    }

    // ========== SECURITY METHODS ==========

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}