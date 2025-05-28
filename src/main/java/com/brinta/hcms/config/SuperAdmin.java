package com.brinta.hcms.config;

import com.brinta.hcms.entity.User;
import com.brinta.hcms.enums.Roles;
import com.brinta.hcms.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SuperAdmin implements CommandLineRunner {

    @Autowired
    private UserRepo userRepo;

    @Override
    public void run(String... args) throws Exception {
        String defaultMail = "superadmin@hcms.com";

        if (!userRepo.existsByEmail(defaultMail)) {
            User superAdmin = new User();
            superAdmin.setUsername("superAdmin@01");
            superAdmin.setEmail(defaultMail);
            superAdmin.setPassword("hcms@1");
            superAdmin.setRole(Roles.SUPER_ADMIN);

            userRepo.save(superAdmin);
        }
    }
}
