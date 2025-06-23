package com.brinta.hcms.controller;

import com.brinta.hcms.dto.PatientDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.Patient;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.request.ForgotPasswordRequest;
import com.brinta.hcms.request.ResetPasswordRequest;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterPatientRequest;
import com.brinta.hcms.request.updateRequest.UpdatePatientRequest;
import com.brinta.hcms.service.PatientService;
import com.brinta.hcms.utility.LoggerUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/patient")
@RequiredArgsConstructor
public class PatientController {

    private static final Class<?> logger = PatientController.class;

    @Autowired
    private PatientService patientService;

    @PostMapping(value = "/online-register", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerOnline(@RequestBody RegisterPatientRequest request) {
        LoggerUtil.info(logger, "Online registration attempt for email: {}",
                request.getEmail());
        try {
            Patient patient = patientService.registerPatientOnline(request);
            LoggerUtil.info(logger, "Online registration successful for email:" +
                    " {}", request.getEmail());
            return ResponseEntity.status(201).body(Map.of("message",
                    "Online patient registered successfully!", "patient", patient));
        } catch (Exception ex) {
            LoggerUtil.error(logger, "Online registration failed for email: {} " +
                    "with error: {}", request.getEmail(), ex.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping(value = "/offline-register", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerOffline(@RequestBody RegisterPatientRequest request) {
        LoggerUtil.info(logger, "Offline registration attempt by ADMIN for email: " +
                "{}", request.getEmail());
        try {
            Patient patient = patientService.registerPatientOffline(request);
            LoggerUtil.info(logger, "Offline registration successful for email:" +
                    " {}", request.getEmail());
            return ResponseEntity.status(201).body(Map.of("message",
                    "Offline patient registered successfully by Admin!",
                    "patient", patient));
        } catch (Exception ex) {
            LoggerUtil.error(logger, "Offline registration failed for email: " +
                    "{} with error: {}", request.getEmail(), ex.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPair> loginPatient(@Valid @RequestBody LoginRequest request) {
        LoggerUtil.info(logger, "Login attempt for email: {}", request.getEmail());
        TokenPair tokenPair = patientService.patientLogin(request);
        LoggerUtil.info(logger, "Login successful for email: {}", request.getEmail());
        return ResponseEntity.ok(tokenPair);
    }

    @Operation(summary = "Forgot Password", description = "Send a password reset link to patient's email")
    @ApiResponse(responseCode = "200", description = "Reset link sent to email if user exists")
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                 HttpServletRequest httpRequest) {
        LoggerUtil.info(logger, "Forgot password API called for email: {}", request.getEmail());
        patientService.forgotPassword(request, httpRequest);
        return ResponseEntity.ok("Password reset link sent to your registered email.");
    }

    @Operation(summary = "Reset Password", description = "Reset patient's password using the token received via email")
    @ApiResponse(responseCode = "200", description = "Password reset successful")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        LoggerUtil.info(logger, "Reset password API called with token: {}", request.getToken());
        patientService.resetPassword(request);
        return ResponseEntity.ok("Password reset successful.");
    }

    @PutMapping(value = "/profile/update/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> updatePatient(@PathVariable Long id, @Valid @RequestBody
    UpdatePatientRequest updatePatientRequest) {
        LoggerUtil.info(logger, "Update attempt for patient ID: {}", id);
        try {
            Patient updatedPatient = patientService.update(id, updatePatientRequest);
            LoggerUtil.info(logger, "Update successful for patient ID: {}", id);
            return ResponseEntity.ok(Map.of("message", "Patient updated successfully!",
                    "patient", updatedPatient));
        } catch (ResourceNotFoundException exception) {
            LoggerUtil.error(logger, "Update failed: {}", exception.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping(value = "/findBy", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> findByParams(@RequestParam(required = false) Long patientId,
                                          @RequestParam(required = false) String contactNumber,
                                          @RequestParam(required = false) String email) {
        try {
            List<PatientDto> patient = patientService.findBy(patientId, contactNumber, email);
            return ResponseEntity.ok(patient);
        } catch (ResourceNotFoundException ex) {
            LoggerUtil.error(logger, "FindBy failed: {}", ex.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            LoggerUtil.error(logger, "Unexpected error occurred: {}", ex.getMessage());
            return ResponseEntity.status(500).body(Map.of("error",
                    "Something went wrong"));
        }
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPatientsRecords(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        var patients = patientService.getWithPagination(page, size);
        return patients.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity
                .ok(Map.of("patients", patients.getContent(),
                        "currentPage", patients.getNumber(),
                        "totalPages", patients.getTotalPages(),
                        "totalElements", patients.getTotalElements()));
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePatientById(@PathVariable("id") Long patientId) {
        LoggerUtil.info(logger, "Delete attempt for patient ID: {}", patientId);
        try {
            patientService.delete(patientId);
            LoggerUtil.info(logger, "Deletion successful for patient ID: {}",
                    patientId);
            return ResponseEntity.ok(Map.of("message", "Patient Deleted successfully!"));
        } catch (ResourceNotFoundException exception) {
            LoggerUtil.error(logger, "Delete failed: {}", exception.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", exception.getMessage()));
        }
    }

}

