package com.brinta.hcms.utility;

import com.brinta.hcms.entity.Agent;
import com.brinta.hcms.entity.User;
import com.brinta.hcms.repository.AgentRepository;
import com.brinta.hcms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;
    private final AgentRepository agentRepository;

    private String extractEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        return principal.toString(); // fallback
    }

    public Object getCurrentActor() {
        String email = extractEmail();

        return userRepository.findByEmail(email)
                .map(user -> (Object) user)
                .or(() -> agentRepository.findByEmail(email).map(agent -> (Object) agent))
                .orElseThrow(() -> new
                        UsernameNotFoundException("No user or agent found with email: " + email));
    }

    public User getCurrentUser() {
        Object actor = getCurrentActor();
        if (actor instanceof User user) {
            return user;
        }
        throw new UsernameNotFoundException("Current authenticated entity is not a User");
    }

    public Agent getCurrentAgent() {
        Object actor = getCurrentActor();
        if (actor instanceof Agent agent) {
            return agent;
        }
        throw new UsernameNotFoundException("Current authenticated entity is not an Agent");
    }

}

