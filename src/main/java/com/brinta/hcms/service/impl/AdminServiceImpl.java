package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.AdminProfileDto;
import com.brinta.hcms.entity.AdminProfile;
import com.brinta.hcms.entity.User;
import com.brinta.hcms.enums.Roles;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.mapper.AdminMapper;
import com.brinta.hcms.repository.AdminRepository;
import com.brinta.hcms.repository.UserRepo;
import com.brinta.hcms.request.registerRequest.RegisterAdminRequest;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.service.AdminService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepo;
    private final UserRepo userRepo;
    private final AdminMapper adminMapper;
//    private final PasswordEncoder passwordEncoder;

    @Override
    public AdminProfile registerAdmin(RegisterAdminRequest registerAdmin) {

        boolean emailExists = adminRepo.existsByEmail(registerAdmin.getEmail());
        boolean contactExists = adminRepo.existsByContactNumber(registerAdmin.getContactNumber());

        if (emailExists || contactExists) {
            StringBuilder errorMessage = new StringBuilder("Already Exists: ");
            if (emailExists) errorMessage.append("Email: ").append(registerAdmin.getEmail());
            if (emailExists && contactExists) errorMessage.append(" | ");
            if (contactExists) errorMessage.append("Contact: ").append(registerAdmin.getContactNumber());
            throw new DuplicateEntryException(errorMessage.toString());
        }

//        String encodedPassword = passwordEncoder.encode(registerAdmin.getPassword());

        User user = new User();
        user.setUsername(registerAdmin.getUserName());
        user.setEmail(registerAdmin.getEmail());
        user.setPassword(registerAdmin.getPassword());
        user.setRole(Roles.ADMIN);

        AdminProfile admin = adminMapper.register(registerAdmin);
        admin.setUser(user);
        user.setAdminProfile(admin);
        userRepo.save(user);

        return admin;
    }

    @Override
    public AdminProfileDto loginAdmin(LoginRequest request) {
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (user.getRole() != Roles.ADMIN) {
            throw new RuntimeException("Access denied: not an admin");
        }

//        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
//            throw new RuntimeException("Invalid email or password");
//        }

        AdminProfile adminProfile = user.getAdminProfile();

        return adminMapper.toDto(adminProfile);
    }

}
