package com.brinta.hcms.config;

import com.brinta.hcms.entity.User;
import com.brinta.hcms.repository.UserRepo;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

@Configuration
@AllArgsConstructor
public class UserDetailsServiceConfig {

    private final UserRepo userRepo;

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

            // Spring Security expects "ROLE_" prefix
            String role = "ROLE_" + user.getRole().name();

            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    List.of(new SimpleGrantedAuthority(role))
            );
        };
    }

}

