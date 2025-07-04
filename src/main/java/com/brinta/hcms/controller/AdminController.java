package com.brinta.hcms.controller;

import com.brinta.hcms.dto.AdminDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.Admin;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.request.ForgotPasswordRequest;
import com.brinta.hcms.request.ResetPasswordRequest;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterAdminRequest;
import com.brinta.hcms.request.registerRequest.RegisterDoctorRequest;
import com.brinta.hcms.service.AdminService;
import com.brinta.hcms.service.DoctorService;
import com.brinta.hcms.service.ForgotPasswordResetService;
import com.brinta.hcms.utility.LoggerUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@AllArgsConstructor
public class AdminController {

    private static final Class<?> logger = PatientController.class;

    @Autowired
    private AdminService adminService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private ForgotPasswordResetService forgotPasswordResetService;

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Register Admin", responses = {
            @ApiResponse(description = "Admin registered successfully",
                    responseCode = "201",
                    content = @Content(schema = @Schema(implementation = Admin.class))),
            @ApiResponse(description = "Email or contact already exists", responseCode = "400")
    })
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody RegisterAdminRequest registerAdminRequest) {
        try {
            Admin admin = adminService.registerAdmin(registerAdminRequest);
            return ResponseEntity.status(201).body(Map.of(
                    "message", "Admin registered successfully!",
                    "admin", admin
            ));
        } catch (DuplicateEntryException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Admin Login", responses = {
            @ApiResponse(description = "Admin logged in successfully", responseCode = "200"),
            @ApiResponse(description = "Invalid credentials", responseCode = "401")
    })
    public ResponseEntity<?> loginAdmin(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            TokenPair tokenPair = adminService.loginAdmin(loginRequest);
            return ResponseEntity.ok(Map.of(
                    "accessToken", tokenPair.getAccessToken(),
                    "refreshToken", tokenPair.getRefreshToken()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Forgot Password", description = "Send a password reset link to admin email")
    @ApiResponse(responseCode = "200", description = "Reset link sent to email if user exists")
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        LoggerUtil.info(logger, "Forgot password API called for email: {}", request.getEmail());
        forgotPasswordResetService.forgotPassword(request, httpRequest);
        return ResponseEntity.ok("Password reset link sent to your registered email.");
    }

    @GetMapping("/reset-password")
    public ResponseEntity<String> validateResetToken(@RequestParam("token") String token) {
        String result = forgotPasswordResetService.validateResetToken(token);
        return result.equals("Token is valid.") ? ResponseEntity.ok(result) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    @Operation(summary = "Reset Password", description = "Reset admin password using the token received via email")
    @ApiResponse(responseCode = "200", description = "Password reset successful")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        LoggerUtil.info(logger, "Reset password API called with token: {}", request.getToken());
        forgotPasswordResetService.resetPassword(request);
        return ResponseEntity.ok("Password reset successful.");
    }

    // ---------------- INTERNAL DOCTOR REGISTRATION ----------------
    @PostMapping(value = "/register/internal", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register Internal Doctor", responses = {
            @ApiResponse(description = "Added internal doctor in the database", responseCode = "201",
                    content = @Content(schema = @Schema(implementation = Doctor.class))),
            @ApiResponse(description = "Email or phone already exists", responseCode = "400")
    })
    public ResponseEntity<?> registerInternalDoctor(@Valid @RequestBody
                                                        RegisterDoctorRequest registerDoctor) {
        try {
            LoggerUtil.info(getClass(), "Registering internal doctor with email: {}",
                    registerDoctor.getEmail());
            Doctor created = doctorService.registerInternalDoctor(registerDoctor);
            return ResponseEntity.status(201).body(Map.of(
                    "message", "Internal doctor registered successfully",
                    "doctor", created
            ));
        } catch (DuplicateEntryException ex) {
            LoggerUtil.warn(getClass(), "Duplicate entry for internal doctor: {}",
                    ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception e) {
            LoggerUtil.error(getClass(), "Error while registering internal doctor", e);
            return ResponseEntity.internalServerError().body(Map.of("error",
                    "Unexpected error occurred"));
        }
    }

    // ---------------- EXTERNAL DOCTOR REGISTRATION ----------------
    @PostMapping(value = "/register/external", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register External Doctor", responses = {
            @ApiResponse(description = "Added external doctor in the database", responseCode = "201",
                    content = @Content(schema = @Schema(implementation = Doctor.class))),
            @ApiResponse(description = "Agent type or email already exists", responseCode = "400"),
            @ApiResponse(description = "Unexpected server error", responseCode = "500")
    })
    public ResponseEntity<?> registerExternalDoctor(@Valid @RequestBody
                                                        RegisterDoctorRequest registerDoctor) {
        try {
            LoggerUtil.info(getClass(),
                    "Registering external doctor with email: {}", registerDoctor.getEmail());

            Doctor created = doctorService.registerExternalDoctor(registerDoctor);

            LoggerUtil.info(getClass(),
                    "External doctor registered successfully: {}", created.getEmail());

            return ResponseEntity.status(201).body(Map.of(
                    "message", "External doctor registered successfully",
                    "doctor", created
            ));

        } catch (ResourceNotFoundException ex) {
            LoggerUtil.warn(getClass(),
                    "Missing agent type for external doctor: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));

        } catch (DuplicateEntryException ex) {
            LoggerUtil.warn(getClass(),
                    "Duplicate entry while registering external doctor: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));

        } catch (Exception ex) {
            LoggerUtil.error(getClass(),
                    "Unexpected error while registering external doctor", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Unexpected error occurred"
            ));
        }
    }

    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get All Admins With Pagination",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of admins",
                            content = @Content(schema = @Schema(implementation = AdminDto.class))),
                    @ApiResponse(responseCode = "204", description = "No admins found")
            })
    public ResponseEntity<?> getAdminRecords(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {

        Page<AdminDto> admins = adminService.getWithPagination(page, size);

        return admins.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(Map.of(
                "admins", admins.getContent(),
                "currentPage", admins.getNumber(),
                "totalPages", admins.getTotalPages(),
                "totalElements", admins.getTotalElements()
        ));
    }

    @DeleteMapping(value = "/user/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Admin",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Admin Deleted Successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request"),
                    @ApiResponse(responseCode = "404", description = "Admin Not Found")
            })
    public ResponseEntity<?> deleteAdminById(@PathVariable("id") Long adminId) {
        try {
            adminService.delete(adminId);
            return ResponseEntity.ok(Map.of("message", "Admin deleted successfully"));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

}

