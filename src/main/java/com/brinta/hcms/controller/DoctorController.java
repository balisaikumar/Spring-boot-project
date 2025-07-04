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
@RequestMapping("/doctor")
@Tag(name = "Doctor Profile API", description = "Operations Related to Doctor")
@AllArgsConstructor
public class DoctorController {

    private static final Class<?> logger = PatientController.class;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private ForgotPasswordResetService forgotPasswordResetService;

    @PostMapping("/login")
    public ResponseEntity<?> doctorLogin(@RequestBody LoginRequest request) {
        try {
            Map<String, Object> response = doctorService.doctorLogin(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).body(Map.of("error", ex.getMessage()));
        }
    }

    @Operation(summary = "Forgot Password", description = "Send a password reset link to doctor's email")
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

    @Operation(summary = "Reset Password", description = "Reset doctor's password using the token received via email")
    @ApiResponse(responseCode = "200", description = "Password reset successful")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        LoggerUtil.info(logger, "Reset password API called with token: {}", request.getToken());
        forgotPasswordResetService.resetPassword(request);
        return ResponseEntity.ok("Password reset successful.");
    }

    @PutMapping(value = "/update/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Doctor", responses = {@ApiResponse(description = "Doctor details updated successfully", responseCode = "200", content = @Content(schema = @Schema(implementation = Doctor.class))), @ApiResponse(description = "Doctor not found", responseCode = "404"), @ApiResponse(description = "Invalid input data", responseCode = "400")})
    public ResponseEntity<?> updateDoctor(@PathVariable Long id, @Valid @RequestBody UpdateDoctorRequest updateDoctorRequest) {
        try {
            Doctor updatedDoctor = doctorService.update(id, updateDoctorRequest);
            return ResponseEntity.ok(Map.of("message", "Doctor updated successfully!", "doctor", updatedDoctor));
        } catch (ResourceNotFoundException exception) {
            return ResponseEntity.status(404).body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping(value = "/findBy", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Doctor By Parameters", responses = {@ApiResponse(responseCode = "200", description = "Doctor found", content = @Content(schema = @Schema(implementation = DoctorDto.class))), @ApiResponse(responseCode = "404", description = "No matching doctor found"), @ApiResponse(responseCode = "400", description = "Enter input field")})
    public ResponseEntity<?> findByParams(@RequestParam(required = false) Long doctorId, @RequestParam(required = false) String contactNumber, @RequestParam(required = false) String email) {
        List<DoctorDto> doctor = doctorService.findBy(doctorId, contactNumber, email);
        if (doctor.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "No doctor found with given criteria."));
        }
        return ResponseEntity.ok(doctor);
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get All Doctors With Pagination",
            responses = {
                    @ApiResponse(description = "List of parents",
                            responseCode = "200",
                            content = @Content(schema = @Schema(implementation = DoctorDto.class))),
                    @ApiResponse(description = "No parents found", responseCode = "404")
            })
    public ResponseEntity<?> getDoctorRecords(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        var parents = doctorService.getWithPagination(page, size);
        return parents.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(Map.of("doctors", parents.getContent(),
                "currentPage", parents.getNumber(),
                "totalPages", parents.getTotalPages(),
                "totalElements", parents.getTotalElements()));

    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Doctor", responses = {@ApiResponse(responseCode = "204", description = "Doctor Deleted Successfully"), @ApiResponse(responseCode = "400", description = "Enter input field"), @ApiResponse(responseCode = "404", description = "Enter the correct ID")})
    public ResponseEntity<?> deleteDoctorById(@PathVariable("id") Long doctorId) {
        try {
            doctorService.delete(doctorId);
            return ResponseEntity.ok(Map.of("message", "Doctor Deleted successfully!"));
        } catch (ResourceNotFoundException exception) {
            return ResponseEntity.status(404).body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping(value = "/appointments", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "List Appointments for Doctor", responses = {@ApiResponse(description = "Fetched all appointments", responseCode = "200", content = @Content(schema = @Schema(implementation = DoctorAppointmentDto.class))), @ApiResponse(description = "Invalid request parameters", responseCode = "400")})
    public ResponseEntity<?> listAppointments(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        try {
            Page<DoctorAppointmentDto> appointments = doctorService.listAppointments(page, size);

            return ResponseEntity.ok(Map.of("message", "Appointments fetched successfully", "appointments", appointments.getContent(), "totalPages", appointments.getTotalPages(), "totalElements", appointments.getTotalElements(), "currentPage", appointments.getNumber()));
        } catch (InvalidRequestException | ResourceNotFoundException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping(value = "rescheduleAppointment/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reschedule DoctorAppointment", responses = {@ApiResponse(description = "DoctorAppointment rescheduled", responseCode = "200"), @ApiResponse(description = "DoctorAppointment not found", responseCode = "404")})
    public ResponseEntity<?> rescheduleAppointment(@PathVariable Long id, @Valid @RequestBody RescheduleAppointmentRequest request) {
        try {
            DoctorAppointmentDto updated = doctorService.rescheduleAppointment(id, request.getNewAppointmentTime());
            return ResponseEntity.ok(Map.of("message", "DoctorAppointment rescheduled successfully", "appointment", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping(value = "cancelAppointment/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel DoctorAppointment", responses = {@ApiResponse(description = "DoctorAppointment cancelled", responseCode = "200"), @ApiResponse(description = "DoctorAppointment not found or unauthorized", responseCode = "404")})
    public ResponseEntity<?> cancelAppointment(@PathVariable Long id) {
        try {
            doctorService.cancelAppointment(id);  // doctorId handled internally using SecurityUtil

            return ResponseEntity.ok(Map.of("message", "Appointment cancelled successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

}

