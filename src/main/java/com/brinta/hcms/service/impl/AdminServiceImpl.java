package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.AdminDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.Admin;
import com.brinta.hcms.entity.User;
import com.brinta.hcms.enums.Roles;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.exception.exceptionHandler.InvalidRequestException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.exception.exceptionHandler.UnAuthException;
import com.brinta.hcms.mapper.AdminMapper;
import com.brinta.hcms.repository.AdminRepository;
import com.brinta.hcms.repository.UserRepository;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterAdminRequest;
import com.brinta.hcms.service.AdminService;
import com.brinta.hcms.service.JwtService;
import com.brinta.hcms.utility.SecurityUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminRepository adminRepo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SecurityUtil securityUtil;

    @Override
    public Admin registerAdmin(RegisterAdminRequest registerAdmin) {

        // Fetch the currently logged-in user
        User currentUser = securityUtil.getCurrentUser();

        // Allow only SUPER_ADMIN to register a new admin
        if (currentUser.getRole() != Roles.SUPER_ADMIN) {
            throw new AccessDeniedException("Only SUPER_ADMIN can register a new admin.");
        }

        // Check for duplicate email or contact
        boolean emailExists = adminRepo.existsByEmail(registerAdmin.getEmail());
        boolean contactExists = adminRepo.existsByContactNumber(registerAdmin.getContactNumber());

        // If either email or contact already exists, throw custom exception
        if (emailExists || contactExists) {
            StringBuilder errorMessage = new StringBuilder("Already Exists: ");
            if (emailExists) errorMessage.append("Email: ").append(registerAdmin.getEmail());
            if (emailExists && contactExists) errorMessage.append(" | ");
            if (contactExists) errorMessage.append("Contact: ")
                    .append(registerAdmin.getContactNumber());
            throw new DuplicateEntryException(errorMessage.toString());
        }

        // Use AdminMapper to convert request into AdminProfile entity
        Admin admin = adminMapper.register(registerAdmin);

        // Create a new User entity and link it with AdminProfile
        User user = new User();
        user.setUsername(registerAdmin.getUserName());
        user.setName(registerAdmin.getName());
        user.setEmail(registerAdmin.getEmail());
        user.setPassword(passwordEncoder.encode(registerAdmin.getPassword()));
        user.setRole(Roles.ADMIN);

        // Link admin and user both ways
        admin.setUser(user);
        user.setAdmin(admin);

        // Save AdminProfile and User to the database
        Admin savedAdmin = adminRepo.save(admin);
        userRepository.save(user);

        return savedAdmin;
    }

    @Override
    public TokenPair loginAdmin(LoginRequest request) {

        // Validate input
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password must be provided");
        }

        // Find admin by email
        Admin admin = adminRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new
                        RuntimeException("Admin not found with email: " + request.getEmail()));

        // Ensure admin has a linked user with credentials
        if (admin.getUser() == null || admin.getUser().getPassword() == null) {
            throw new
                    RuntimeException("User credentials are missing for admin: "
                    + request.getEmail());
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), admin.getUser().getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Authenticate using Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(admin.getUser().getEmail(),
                        request.getPassword())
        );

        // Generate and return JWT token pair
        return jwtService.generateTokenPair(authentication);
    }

    @Override
    public Page<AdminDto> getWithPagination(int page, int size) {

        if (page < 0 || size <= 0) {
            throw new
                    InvalidRequestException("Page index must not be negative and size " +
                    "must be greater than zero.");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Admin> adminPage = adminRepo.findAll(pageable);

        if (adminPage.isEmpty()) {
            return Page.empty();
        }

        return adminPage.map(adminMapper::toDto);

    }

    @Override
    public void delete(Long adminId) {

        // Get currently logged-in user
        User currentUser = securityUtil.getCurrentUser();

        // If SUPER_ADMIN: allow deletion of any admin
        if (currentUser.getRole() == Roles.SUPER_ADMIN) {
            Admin admin = adminRepo.findById(adminId)
                    .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
            adminRepo.delete(admin);
            return;
        }

        // If ADMIN: only allow deletion of their own profile
        if (currentUser.getRole() == Roles.ADMIN) {
            Admin admin = adminRepo.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new
                            ResourceNotFoundException("Admin profile not linked to current user"));

            if (!admin.getId().equals(adminId)) {
                throw new UnAuthException("You are not authorized to delete another admin profile.");
            }

            adminRepo.delete(admin);
            return;
        }

        // For any other role, deny access
        throw new AccessDeniedException("You are not authorized to delete admin profiles.");
    }

}

