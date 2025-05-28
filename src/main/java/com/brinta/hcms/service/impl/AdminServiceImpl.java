package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.AdminProfileDto;
import com.brinta.hcms.entity.AdminProfile;
import com.brinta.hcms.entity.User;
import com.brinta.hcms.enums.Roles;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.mapper.AdminMapper;
import com.brinta.hcms.repository.AdminRepository;
import com.brinta.hcms.repository.UserRepo;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterAdminRequest;
import com.brinta.hcms.service.AdminService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminRepository adminRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

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

        // Create and save User entity
        User user = new User();
        user.setUsername(registerAdmin.getUserName());
        user.setEmail(registerAdmin.getEmail());
        user.setPassword(passwordEncoder.encode(registerAdmin.getPassword()));
        user.setRole(Roles.ADMIN);

        // Create AdminProfile
        AdminProfile admin = adminMapper.register(registerAdmin);
        admin.setUser(user);
        user.setAdminProfile(admin);

        // First save Admin to generate ID if needed
        AdminProfile savedAdmin = adminRepo.save(admin);

        // Save User with a link to Admin
        userRepo.save(user);

        return savedAdmin;
    }

    @Override
    public AdminProfileDto loginAdmin(LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password must be provided");
        }

        AdminProfile admin = adminRepo.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Admin not found with email: " + request.getEmail()));

        if (admin.getUser() == null || admin.getUser().getPassword() == null) {
            throw new RuntimeException("User credentials are missing for admin: " + request.getEmail());
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getUser().getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return adminMapper.toDto(admin);
    }

}

