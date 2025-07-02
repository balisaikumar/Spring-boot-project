package com.brinta.hcms.controller;

import com.brinta.hcms.dto.DoctorAppointmentDto;
import com.brinta.hcms.dto.DoctorDto;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.exception.exceptionHandler.InvalidRequestException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.request.ForgotPasswordRequest;
import com.brinta.hcms.request.ResetPasswordRequest;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.updateRequest.RescheduleAppointmentRequest;
import com.brinta.hcms.request.updateRequest.UpdateDoctorRequest;
import com.brinta.hcms.service.DoctorService;
import com.brinta.hcms.service.ForgotPasswordResetService;
import com.brinta.hcms.utility.LoggerUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/doctor")
@Tag(name = "Doctor Profile API", description = "Operations Related to Doctor")
@AllArgsConstructor
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private ForgotPasswordResetService forgotPasswordResetService;

    @PostMapping(value = "/login",produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Login for doctors", responses = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    public ResponseEntity<?> doctorLogin(@RequestBody LoginRequest request) {
        String maskedEmail = LoggerUtil.mask(request.getEmail());
        log.info("Doctor login attempt with email: {}", maskedEmail);
        try {
            Map<String, Object> response = doctorService.doctorLogin(request);
            log.info("Doctor login successful for email: {}", maskedEmail);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            log.warn("Doctor login failed for email: {} - Reason: {}", maskedEmail, ex.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", ex.getMessage()));
        }
    }

    @Operation(summary = "Forgot Password", description = "Send a password reset link to doctor's email")
    @ApiResponse(responseCode = "200", description = "Reset link sent to email if user exists")
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        log.info("Forgot password API called for email: {}", request.getEmail());
        forgotPasswordResetService.forgotPassword(request, httpRequest);
        return ResponseEntity.ok("Password reset link sent to your registered email.");
    }

    @GetMapping("/reset-password")
    public ResponseEntity<String> validateResetToken(@RequestParam("token") String token) {
        String result = forgotPasswordResetService.validateResetToken(token);
        return result.equals("Token is valid.") ? ResponseEntity.ok(result) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    @Operation(summary = "Reset Password", description = "Reset doctor's password using the token received via email")
    @ApiResponse(responseCode = "200", description = "Password reset successful")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password API called with token: {}", request.getToken());
        forgotPasswordResetService.resetPassword(request);
        return ResponseEntity.ok("Password reset successful.");
    }

    @PutMapping(value = "/update/{id}",produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Update doctor profile", responses = {
            @ApiResponse(responseCode = "200", description = "Doctor updated successfully"),
            @ApiResponse(responseCode = "404", description = "Doctor not found", content = @Content)
    })
    public ResponseEntity<?> updateDoctor(@PathVariable Long id,
                                          @Valid @RequestBody UpdateDoctorRequest updateRequest) {
        log.info("Request to update doctor profile with ID: {}", id);
        try {
            Doctor updatedDoctor = doctorService.update(id, updateRequest);
            log.info("Doctor profile updated successfully for ID: {}", id);
            return ResponseEntity.ok(Map.of("message", "Doctor updated successfully!", "doctor", updatedDoctor));
        } catch (ResourceNotFoundException exception) {
            log.warn("Update failed: {}", exception.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping(value = "/findBy",produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Find doctor by ID, email or phone", responses = {
            @ApiResponse(responseCode = "200", description = "Doctor(s) found"),
            @ApiResponse(responseCode = "404", description = "No doctor found", content = @Content)
    })
    public ResponseEntity<?> findByParams(@RequestParam(required = false) Long doctorId,
                                          @RequestParam(required = false) String contactNumber,
                                          @RequestParam(required = false) String email) {
        log.info("Find doctor with params - ID: {}, Contact: {}, Email: {}", doctorId,
                LoggerUtil.mask(contactNumber), LoggerUtil.mask(email));
        List<DoctorDto> doctors = doctorService.findBy(doctorId, contactNumber, email);
        if (doctors.isEmpty()) {
            log.warn("No doctor found with provided parameters.");
            return ResponseEntity.status(404).body(Map.of("error", "No doctor found with given criteria."));
        }
        log.info("Doctors found with given criteria: {}", doctors.size());
        return ResponseEntity.ok(doctors);
    }

    @GetMapping(value = "/all",produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Get paginated list of doctors", responses = {
            @ApiResponse(responseCode = "200", description = "Doctors retrieved successfully"),
            @ApiResponse(responseCode = "204", description = "No doctors found", content = @Content)
    })
    public ResponseEntity<?> getDoctorRecords(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching doctor records with pagination - Page: {}, Size: {}", page, size);
        Page<DoctorDto> doctors = doctorService.getWithPagination(page, size);
        if (doctors.isEmpty()) {
            log.info("No doctors found for pagination Page: {}, Size: {}", page, size);
            return ResponseEntity.noContent().build();
        }
        log.info("Fetched {} doctor(s) on page {}", doctors.getNumberOfElements(), doctors.getNumber());
        return ResponseEntity.ok(Map.of(
                "doctors", doctors.getContent(),
                "currentPage", doctors.getNumber(),
                "totalPages", doctors.getTotalPages(),
                "totalElements", doctors.getTotalElements()
        ));
    }

    @DeleteMapping(value = "/delete/{id}",produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Delete doctor by ID", responses = {
            @ApiResponse(responseCode = "200", description = "Doctor deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Doctor not found", content = @Content)
    })
    public ResponseEntity<?> deleteDoctorById(@PathVariable("id") Long doctorId) {
        log.info("Attempting to delete doctor profile with ID: {}", doctorId);
        try {
            doctorService.delete(doctorId);
            log.info("Doctor profile deleted successfully for ID: {}", doctorId);
            return ResponseEntity.ok(Map.of("message", "Doctor deleted successfully!"));
        } catch (ResourceNotFoundException exception) {
            log.warn("Delete failed: {}", exception.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping(value = "/appointments",produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "List appointments for the doctor", responses = {
            @ApiResponse(responseCode = "200", description = "Appointments fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Appointments not found", content = @Content)
    })
    public ResponseEntity<?> listAppointments(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        log.info("Doctor is requesting appointment list - Page: {}, Size: {}", page, size);
        try {
            Page<DoctorAppointmentDto> appointments = doctorService.listAppointments(page, size);
            log.info("Found {} appointments on page {}", appointments.getNumberOfElements(), appointments.getNumber());
            return ResponseEntity.ok(Map.of(
                    "message", "Appointments fetched successfully",
                    "appointments", appointments.getContent(),
                    "totalPages", appointments.getTotalPages(),
                    "totalElements", appointments.getTotalElements(),
                    "currentPage", appointments.getNumber()
            ));
        } catch (InvalidRequestException | ResourceNotFoundException e) {
            log.warn("Failed to list appointments: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping(value = "/rescheduleAppointment/{id}",produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Reschedule a doctor appointment", responses = {
            @ApiResponse(responseCode = "200", description = "Appointment rescheduled successfully"),
            @ApiResponse(responseCode = "404", description = "Appointment not found", content = @Content)
    })
    public ResponseEntity<?> rescheduleAppointment(@PathVariable Long id,
                                                   @Valid @RequestBody RescheduleAppointmentRequest request) {
        log.info("Reschedule request for appointment ID: {} to time: {}", id, request.getNewAppointmentTime());
        try {
            DoctorAppointmentDto updated = doctorService.rescheduleAppointment(id, request.getNewAppointmentTime());
            log.info("Appointment ID: {} rescheduled successfully", id);
            return ResponseEntity.ok(Map.of(
                    "message", "Appointment rescheduled successfully",
                    "appointment", updated
            ));
        } catch (RuntimeException e) {
            log.warn("Failed to reschedule appointment ID: {} - Reason: {}", id, e.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping(value = "/cancelAppointment/{id}",produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Cancel a doctor appointment", responses = {
            @ApiResponse(responseCode = "200", description = "Appointment cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Appointment not found", content = @Content)
    })
    public ResponseEntity<?> cancelAppointment(@PathVariable Long id) {
        log.info("Cancel appointment request received for ID: {}", id);
        try {
            doctorService.cancelAppointment(id);
            log.info("Appointment ID: {} cancelled successfully", id);
            return ResponseEntity.ok(Map.of("message", "Appointment cancelled successfully"));
        } catch (RuntimeException e) {
            log.warn("Failed to cancel appointment ID: {} - Reason: {}", id, e.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

}

