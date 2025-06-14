package com.brinta.hcms.controller;

import com.brinta.hcms.dto.AdminDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.Admin;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
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
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

    @PostMapping(value = "/doctor/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register Doctor", responses = {
            @ApiResponse(description = "Added Doctor in the database",
                    responseCode = "201",
                    content = @Content(schema = @Schema(implementation = Doctor.class))),
            @ApiResponse(description = "Email already exists", responseCode = "400")})
    public ResponseEntity<?> registerDoctor(@Valid @RequestBody RegisterDoctorRequest registerDoctor) {
        try {
            Doctor createdParent = doctorService.register(registerDoctor);
            return ResponseEntity.status(201)
                    .body(Map.of("message", "Doctor registered successfully!",
                            "doctor", createdParent));
        } catch (DuplicateEntryException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
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
