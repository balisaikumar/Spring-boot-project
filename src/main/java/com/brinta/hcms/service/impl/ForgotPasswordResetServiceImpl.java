package com.brinta.hcms.service.impl;

import com.brinta.hcms.entity.User;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.repository.UserRepository;
import com.brinta.hcms.request.ForgotPasswordRequest;
import com.brinta.hcms.request.ResetPasswordRequest;
import com.brinta.hcms.service.EmailService;
import com.brinta.hcms.service.ForgotPasswordResetService;
import com.brinta.hcms.utility.LoggerUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ForgotPasswordResetServiceImpl implements ForgotPasswordResetService {

    private static final Class<?> logger = ForgotPasswordResetServiceImpl.class;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        LoggerUtil.info(logger, "Forgot password attempt for email: {}",
                request.getEmail());

        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isEmpty()) {
            LoggerUtil.warn(logger, "User not found with email: {}",
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

        LoggerUtil.info(logger, "Reset password email sent to: {}",
                user.getEmail());
    }

    @Override
    public String validateResetToken(String token) {
        LoggerUtil.info(logger, "Validating reset token: {}", token);

        Optional<User> optionalUser = userRepository.findByResetToken(token);
        if (optionalUser.isEmpty()) {
            LoggerUtil.warn(logger, "Invalid reset token: {}", token);
            return "Invalid token.";
        }

        User user = optionalUser.get();
        if (user.getTokenExpiryDate() != null && user.getTokenExpiryDate()
                .isBefore(LocalDateTime.now())) {
            LoggerUtil.warn(logger, "Expired reset token for user: {}",
                    user.getEmail());
            return "Token expired.";
        }

        LoggerUtil.info(logger, "Reset token is valid for user: {}",
                user.getEmail());
        return "Token is valid.";
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        LoggerUtil.info(logger, "Reset password request received with token:" +
                " {}", request.getToken());

        Optional<User> optionalUser = userRepository
                .findByResetToken(request.getToken());
        if (optionalUser.isEmpty()) {
            LoggerUtil.warn(logger, "Reset failed: Invalid token: {}",
                    request.getToken());
            throw new ResourceNotFoundException("Invalid password reset token");
        }

        User user = optionalUser.get();

        if (user.getTokenExpiryDate() != null && user.getTokenExpiryDate()
                .isBefore(LocalDateTime.now())) {
            LoggerUtil.warn(logger, "Reset failed: Token expired for user: {}",
                    user.getEmail());
            throw new RuntimeException("Token has expired. Request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setTokenExpiryDate(null);
        userRepository.save(user);

        LoggerUtil.info(logger, "Password reset successful for user: {}",
                user.getEmail());
    }

}

