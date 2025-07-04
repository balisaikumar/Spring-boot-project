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
import io.swagger.v3.oas.annotations.media.Schema;
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

    private static final Class<?> logger = PatientController.class;

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Doctor Login",
            description = "Allows internal and external doctors to login using email and password.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Login successful",
                            content = @Content(schema = @Schema(example = """
                                {
                                  "accessToken": "jwt-access-token",
                                  "refreshToken": "jwt-refresh-token",
                                  "role": "INTERNAL_DOCTOR | EXTERNAL_DOCTOR"
                                }
                            """))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request or credentials"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Server error"
                    )
            }
    )
    public ResponseEntity<?> loginDoctor(@Valid @RequestBody LoginRequest request) {
        LoggerUtil.info(getClass(),
                "Login request received for doctor: {}", request.getEmail());

        try {
            Map<String, Object> tokenResponse = doctorService.doctorLogin(request);

            LoggerUtil.info(getClass(),
                    "Login successful for doctor: {}", request.getEmail());
            return ResponseEntity.ok(tokenResponse);

        } catch (InvalidRequestException ex) {
            LoggerUtil.warn(getClass(),
                    "Validation failed for doctor login: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));

        } catch (RuntimeException e) {
            LoggerUtil.warn(getClass(),
                    "Login failed for doctor [{}]: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));

        } catch (Exception ex) {
            LoggerUtil.error(getClass(), "Unexpected error during doctor login", ex);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Unexpected server error"));
        }
    }

    @Operation(summary = "Forgot Password", description = "Send a password reset " +
            "link to doctor's email")
    @ApiResponse(responseCode = "200", description = "Reset link sent to email if user exists")
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                 HttpServletRequest httpRequest) {
        LoggerUtil.info(logger, "Forgot password API called for email: {}",
                request.getEmail());

        forgotPasswordResetService.forgotPassword(request, httpRequest);
        return ResponseEntity.ok("Password reset link sent to your registered email.");
    }

    @GetMapping("/reset-password")
    public ResponseEntity<String> validateResetToken(@RequestParam("token") String token) {
        String result = forgotPasswordResetService.validateResetToken(token);
        return result.equals("Token is valid.")
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    @Operation(summary = "Reset Password", description = "Reset doctor's password using the " +
            "token received via email")
    @ApiResponse(responseCode = "200", description = "Password reset successful")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        LoggerUtil.info(logger, "Reset password API called with token: {}",
                request.getToken());

        forgotPasswordResetService.resetPassword(request);
        return ResponseEntity.ok("Password reset successful.");
    }

    @PutMapping(value = "/update/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'EXTERNAL_DOCTOR')")
    @Operation(summary = "Update Doctor", responses = {@ApiResponse
            (description = "Doctor details updated successfully",
                    responseCode = "200", content = @Content(schema = @Schema
                    (implementation = Doctor.class))),
            @ApiResponse(description = "Doctor not found", responseCode = "404"),
            @ApiResponse(description = "Invalid input data", responseCode = "400")})
    public ResponseEntity<?> updateDoctor(@PathVariable Long id, @Valid @RequestBody
    UpdateDoctorRequest updateDoctorRequest) {
        try {
            Doctor updatedDoctor = doctorService.update(id, updateDoctorRequest);
            return ResponseEntity.ok(Map.of("message", "Doctor updated successfully!",
                    "doctor", updatedDoctor));
        } catch (ResourceNotFoundException exception) {
            log.warn("Update failed: {}", exception.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping(value = "/findBy", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Doctor By Parameters",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Doctor found",
                            content = @Content(schema = @Schema(implementation = DoctorDto.class))),
                    @ApiResponse(responseCode = "404",
                            description = "No matching doctor found"),
                    @ApiResponse(responseCode = "400",
                            description = "Enter input field")
            })
    public ResponseEntity<?> findByParams(@RequestParam(required = false) Long doctorId,
                                          @RequestParam(required = false) String contactNumber,
                                          @RequestParam(required = false) String email) {
        List<DoctorDto> doctor = doctorService.findBy(doctorId, contactNumber, email);
        if (doctor.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error",
                    "No doctor found with given criteria."));
        }
        log.info("Doctors found with given criteria: {}", doctor.size());
        return ResponseEntity.ok(doctor);
    }
    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get All Doctors With Pagination",
            responses = {
                    @ApiResponse(description = "List of Doctors",
                            responseCode = "200",
                            content = @Content(schema = @Schema(implementation = DoctorDto.class))),
                    @ApiResponse(description = "No doctors found", responseCode = "404")
            })
    public ResponseEntity<?> getDoctorRecords(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        var doctors = doctorService.getWithPagination(page, size);
        return doctors.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(Map.of("doctors", doctors.getContent(),
                "currentPage", doctors.getNumber(),
                "totalPages", doctors.getTotalPages(),
                "totalElements", doctors.getTotalElements()));

    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'EXTERNAL_DOCTOR')")
    @Operation(summary = "Delete Doctor",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Doctor Deleted Successfully"),
                    @ApiResponse(responseCode = "400", description = "Enter input field"),
                    @ApiResponse(responseCode = "404", description = "Enter the correct ID")
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

