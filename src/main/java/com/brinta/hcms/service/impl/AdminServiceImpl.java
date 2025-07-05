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
import com.brinta.hcms.utility.LoggerUtil;
import com.brinta.hcms.utility.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Transactional
@AllArgsConstructor
@NoArgsConstructor
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
        User currentUser = securityUtil.getCurrentUser();
        log.info("Attempting to register new admin by user: {}", currentUser.getUsername());

        if (currentUser.getRole() != Roles.SUPER_ADMIN) {
            log.warn("Access denied: User {} with role {} attempted to register an admin.",
                    currentUser.getUsername(), currentUser.getRole());
            throw new AccessDeniedException("Only SUPER_ADMIN can register a new admin.");
        }

        String maskedEmail = LoggerUtil.mask(registerAdmin.getEmail());
        String maskedContact = LoggerUtil.mask(registerAdmin.getContactNumber());

        boolean emailExists = adminRepo.existsByEmail(registerAdmin.getEmail());
        boolean contactExists = adminRepo.existsByContactNumber(registerAdmin.getContactNumber());

        if (emailExists || contactExists) {
            StringBuilder errorMessage = new StringBuilder("Already Exists: ");
            if (emailExists) errorMessage.append("Email: ").append(maskedEmail);
            if (emailExists && contactExists) errorMessage.append(" | ");
            if (contactExists) errorMessage.append("Contact: ").append(maskedContact);

            log.warn("Admin registration failed due to duplicate entry: {}",
                    errorMessage.toString());
            throw new DuplicateEntryException(errorMessage.toString());
        }

        Admin admin = adminMapper.register(registerAdmin);

        User user = new User();
        user.setUsername(registerAdmin.getUserName());
        user.setName(registerAdmin.getName());
        user.setEmail(registerAdmin.getEmail());
        user.setPassword(passwordEncoder.encode(registerAdmin.getPassword())); // Do NOT log
        user.setRole(Roles.ADMIN);

        admin.setUser(user);
        user.setAdmin(admin);

        Admin savedAdmin = adminRepo.save(admin);
        userRepository.save(user);

        log.info("Admin successfully registered: username={}, email={}, contact={}",
                registerAdmin.getUserName(), maskedEmail, maskedContact);

        return savedAdmin;
    }

    @Override
    public TokenPair loginAdmin(LoginRequest request) {
        String maskedEmail = LoggerUtil.mask(request.getEmail());

        if (request.getEmail() == null || request.getPassword() == null) {
            log.warn("Admin login failed: email={}, reason={}", maskedEmail,
                    "Missing email or password");
            throw new IllegalArgumentException("Email and password must be provided");
        }

        log.info("Login attempt for admin with email: {}", maskedEmail);

        Admin admin = adminRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Admin login failed: email={}, reason={}", maskedEmail,
                            "Admin not found");
                    return new RuntimeException("Admin not found with email: " + maskedEmail);
                });

        if (admin.getUser() == null || admin.getUser().getPassword() == null) {
            log.error("Admin login failed: email={}, reason={}",
                    maskedEmail, "Missing credentials");
            throw new RuntimeException("User credentials are missing for admin: " + maskedEmail);
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getUser().getPassword())) {
            log.warn("Admin login failed: email={}, reason={}", maskedEmail, "Invalid password");
            throw new RuntimeException("Invalid password");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(admin.getUser().getEmail(),
                        request.getPassword())
        );

        log.info("Admin logged in successfully: email={}", maskedEmail);
        return jwtService.generateTokenPair(authentication);
    }

    @Override
    public Page<AdminDto> getWithPagination(int page, int size) {
        log.info("Fetching paginated admin list: page={}, size={}", page, size);

        if (page < 0 || size <= 0) {
            log.warn("Invalid pagination parameters: page={}, size={}", page, size);
            throw new InvalidRequestException("Page index must not be negative and size " +
                    "must be greater than zero.");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Admin> adminPage = adminRepo.findAll(pageable);

        if (adminPage.isEmpty()) {
            log.info("No admin records found for page={}, size={}", page, size);
            return Page.empty();
        }

        log.info("Admin records retrieved successfully: count={}, page={}, size={}",
                adminPage.getNumberOfElements(), page, size);

        return adminPage.map(adminMapper::toDto);
    }

    @Override
    public void delete(Long adminId) {
        User currentUser = securityUtil.getCurrentUser();
        log.info("Delete admin request: requestedByUser={}, role={}, targetAdminId={}",
                currentUser.getUsername(), currentUser.getRole(), adminId);

        if (currentUser.getRole() == Roles.SUPER_ADMIN) {
            Admin admin = adminRepo.findById(adminId)
                    .orElseThrow(() -> {
                        log.warn("Admin delete failed: Admin ID {} not found (by SUPER_ADMIN)",
                                adminId);
                        return new ResourceNotFoundException("Admin not found");
                    });

            adminRepo.delete(admin);
            log.info("Admin deleted successfully by SUPER_ADMIN: adminId={}", adminId);
            return;
        }

        if (currentUser.getRole() == Roles.ADMIN) {
            Admin admin = adminRepo.findByUserId(currentUser.getId())
                    .orElseThrow(() -> {
                        log.error("Delete failed: Admin profile not found for userId={}",
                                currentUser.getId());
                        return new ResourceNotFoundException("Admin profile not linked to " +
                                "current user");
                    });

            if (!admin.getId().equals(adminId)) {
                log.warn("Unauthorized delete attempt by ADMIN userId={} for adminId={}",
                        currentUser.getId(), adminId);
                throw new UnAuthException("You are not authorized to delete another admin profile.");
            }

            adminRepo.delete(admin);
            log.info("Admin deleted their own profile: adminId={}, userId={}", adminId,
                    currentUser.getId());
            return;
        }

        log.warn("Access denied: User {} with role {} attempted to delete adminId={}",
                currentUser.getUsername(), currentUser.getRole(), adminId);
        throw new AccessDeniedException("You are not authorized to delete admin profiles.");
    }

}

