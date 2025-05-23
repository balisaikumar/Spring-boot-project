package com.brinta.hcms.config;

import com.brinta.hcms.entity.User;
import com.brinta.hcms.enums.Roles;
import com.brinta.hcms.repository.UserRepo;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DefaultAdmin implements CommandLineRunner {

    private final UserRepo userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByEmail("SaiKumar09@gmail.com")){
            User admin = new User();
            admin.setEmail("SaiKumar09@gmail.com");
            admin.setPassword(passwordEncoder.encode("SaiKumar"));
            admin.setUsername("Naruto");
            admin.setRole(Roles.ADMIN);

            userRepository.save(admin);
        }
    }

}

