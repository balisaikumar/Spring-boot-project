package com.brinta.hcms.service.impl;

import com.brinta.hcms.entity.User;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.repository.UserRepository;
import com.brinta.hcms.request.ForgotPasswordRequest;
import com.brinta.hcms.request.ResetPasswordRequest;
import com.brinta.hcms.service.EmailService;
import com.brinta.hcms.service.ForgotPasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ForgotPasswordResetServiceImpl implements ForgotPasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        log.info("Forgot password attempt for email: {}",
                request.getEmail());

        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isEmpty()) {
            log.warn( "User not found with email: {}",
                    request.getEmail());
            throw new ResourceNotFoundException("User not found with email: " +
                    request.getEmail());
        }

        User user = optionalUser.get();
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setTokenExpiryDate(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() +
                ":" + httpRequest.getServerPort();
        String path = switch (user.getRole()) {
            case ADMIN -> "/admin/reset-password";
            case DOCTOR -> "/doctor/reset-password";
            case PATIENT -> "/patient/reset-password";
            default -> throw new IllegalStateException("Unexpected value: " +
                    user.getRole());
        };
        String resetLink = baseUrl + path + "?token=" + token;

        String emailContent = "Dear " + user.getUsername() + ",\n\n" +
                "To reset your password, click the following link:\n" + resetLink
                + "\n\nThis link will expire in 15 minutes." +
                "\n\nRegards,\nHealthcare System Team";

        emailService.sendEmail(user.getEmail(), "Password Reset Request",
                emailContent);

        log.info("Reset password email sent to: {}",
                user.getEmail());
    }

    @Override
    public String validateResetToken(String token) {
        log.info("Validating reset token: {}", token);

        Optional<User> optionalUser = userRepository.findByResetToken(token);
        if (optionalUser.isEmpty()) {
            log.warn("Invalid reset token: {}", token);
            return "Invalid token.";
        }

        User user = optionalUser.get();
        if (user.getTokenExpiryDate() != null && user.getTokenExpiryDate()
                .isBefore(LocalDateTime.now())) {
            log.warn("Expired reset token for user: {}",
                    user.getEmail());
            return "Token expired.";
        }

        log.info("Reset token is valid for user: {}",
                user.getEmail());
        return "Token is valid.";
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Reset password request received with token:" +
                " {}", request.getToken());

        Optional<User> optionalUser = userRepository
                .findByResetToken(request.getToken());
        if (optionalUser.isEmpty()) {
            log.warn("Reset failed: Invalid token: {}",
                    request.getToken());
            throw new ResourceNotFoundException("Invalid password reset token");
        }

        User user = optionalUser.get();

        if (user.getTokenExpiryDate() != null && user.getTokenExpiryDate()
                .isBefore(LocalDateTime.now())) {
            log.warn("Reset failed: Token expired for user: {}",
                    user.getEmail());
            throw new RuntimeException("Token has expired. Request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setTokenExpiryDate(null);
        userRepository.save(user);

        log.info("Password reset successful for user: {}",
                user.getEmail());
    }

}

