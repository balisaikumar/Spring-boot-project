package com.brinta.hcms.service;

import com.brinta.hcms.entity.Agent;
import com.brinta.hcms.entity.User;
import com.brinta.hcms.repository.AgentRepository;
import com.brinta.hcms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    private final AgentRepository agentRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
        }

        // Handle Agent as login user (e.g., EXTERNAL_DOCTOR, etc.)
        Optional<Agent> agentOpt = agentRepository.findByEmail(email);
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            return new org.springframework.security.core.userdetails.User(
                    agent.getEmail(),
                    agent.getPassword(),
                    List.of(
                            // e.g. ROLE_PHARMACY
                            new SimpleGrantedAuthority("ROLE_" + agent.getAgentType().name()),

                            // Generic access for all agent types
                            new SimpleGrantedAuthority("ROLE_AGENT")
                    )
            );
        }

        throw new UsernameNotFoundException("No user or agent found with email: " + email);
    }

}

