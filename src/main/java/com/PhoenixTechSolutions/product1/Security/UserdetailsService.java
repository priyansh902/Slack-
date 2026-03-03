package com.PhoenixTechSolutions.product1.Security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.PhoenixTechSolutions.product1.repositiory.UserRepositiory;

@Service
public class UserdetailsService implements UserDetailsService {

    private final UserRepositiory userRepository;

    public UserdetailsService(UserRepositiory userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        return userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + email));
    }
    
}
