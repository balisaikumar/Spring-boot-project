package com.brinta.hcms.config;

import com.brinta.hcms.entity.User;
import com.brinta.hcms.enums.Roles;
import com.brinta.hcms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
public class SuperAdmin implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String defaultMail = "superadmin@hcms.com";

        if (!userRepository.existsByEmail(defaultMail)) {
            User superAdmin = new User();
            superAdmin.setUsername("superAdmin@01");
            superAdmin.setEmail(defaultMail);
            superAdmin.setPassword(passwordEncoder.encode("hcms@1"));
            superAdmin.setRole(Roles.SUPER_ADMIN);

            userRepository.save(superAdmin);
        }
    }
}

