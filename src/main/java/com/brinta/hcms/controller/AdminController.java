package com.brinta.hcms.controller;

import com.brinta.hcms.entity.AdminProfile;
import com.brinta.hcms.entity.DoctorProfile;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterAdminRequest;
import com.brinta.hcms.request.registerRequest.RegisterDoctorRequest;
import com.brinta.hcms.service.AdminService;
import com.brinta.hcms.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@AllArgsConstructor
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private DoctorService doctorService;

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Register Admin", responses = {
            @ApiResponse(description = "Admin registered successfully",
                    responseCode = "201",
                    content = @Content(schema = @Schema(implementation = AdminProfile.class))),
            @ApiResponse(description = "Email or contact already exists", responseCode = "400")
    })
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody RegisterAdminRequest registerAdminRequest) {
        try {
            AdminProfile adminProfile = adminService.registerAdmin(registerAdminRequest);
            return ResponseEntity.status(201).body(Map.of(
                    "message", "Admin registered successfully!",
                    "admin", adminProfile
            ));
        } catch (DuplicateEntryException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/doctor/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register Doctor", responses = {
            @ApiResponse(description = "Added Doctor in the database",
                    responseCode = "201",
                    content = @Content(schema = @Schema(implementation = DoctorProfile.class))),
            @ApiResponse(description = "Email already exists", responseCode = "400")})
    public ResponseEntity<?> create(@Valid @RequestBody RegisterDoctorRequest registerDoctor) {
        try {
            DoctorProfile createdParent = doctorService.register(registerDoctor);
            return ResponseEntity.status(201)
                    .body(Map.of("message", "Doctor registered successfully!",
                            "doctor", createdParent));
        } catch (DuplicateEntryException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Admin Login", responses = {
            @ApiResponse(description = "Admin logged in successfully", responseCode = "200"),
            @ApiResponse(description = "Invalid credentials", responseCode = "401")
    })
    public ResponseEntity<?> loginAdmin(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            var adminDto = adminService.loginAdmin(loginRequest);
            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "admin", adminDto
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

}

