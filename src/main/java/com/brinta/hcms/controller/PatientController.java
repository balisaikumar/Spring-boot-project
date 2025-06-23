package com.brinta.hcms.controller;

import com.brinta.hcms.dto.DoctorDto;
import com.brinta.hcms.dto.PatientDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.Patient;
import com.brinta.hcms.exception.exceptionHandler.EmailAlreadyExistsException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterPatientRequest;
import com.brinta.hcms.request.updateRequest.UpdatePatientRequest;
import com.brinta.hcms.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Autowired
    private PatientService patientService;

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register Patient", responses = {
            @ApiResponse(responseCode = "201", description = "Patient registered successfully",
                    content = @Content(schema = @Schema(implementation = Patient.class))),
            @ApiResponse(responseCode = "409", description = "Email or contact already exists")
    })
    public ResponseEntity<?> registerPatient(@Valid @RequestBody RegisterPatientRequest request) {
        try {
            Patient patient = patientService.registerPatient(request);
            return ResponseEntity.status(201).body(
                    Map.of(
                            "message", "Patient registered successfully!",
                            "patient", patient
                    )
            );
        } catch (EmailAlreadyExistsException e) {
            return ResponseEntity.status(409).body(
                    Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Something went wrong")
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPair> loginPatient(@Valid @RequestBody LoginRequest request) {
        TokenPair tokenPair = patientService.patientLogin(request);
        return ResponseEntity.ok(tokenPair);
    }

    @PutMapping(value = "/profile/update/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Update Patient",
            responses = {
                    @ApiResponse(description = "Patient details updated successfully",
                            responseCode = "200",
                            content = @Content(schema = @Schema(implementation = Patient.class))),
                    @ApiResponse(description = "Patient not found", responseCode = "404"),
                    @ApiResponse(description = "Invalid input data", responseCode = "400")
            })
    public ResponseEntity<?> updatePatient(@PathVariable Long id,
                                           @Valid @RequestBody UpdatePatientRequest updatePatientRequest) {
        try {
            Patient updatedPatient = patientService.update(id, updatePatientRequest);
            return ResponseEntity.ok(Map.of(
                    "message", "Patient updated successfully!",
                    "patient", updatedPatient
            ));
        } catch (ResourceNotFoundException exception) {
            return ResponseEntity.status(404).body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping(value = "/findBy", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Patient By Parameters",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Patient found",
                            content = @Content(schema = @Schema(implementation = PatientDto.class))),
                    @ApiResponse(responseCode = "404",
                            description = "No matching patient found"),
                    @ApiResponse(responseCode = "400",
                            description = "Enter input field")
            })
    public ResponseEntity<?> findByParams(@RequestParam(required = false) Long patientId,
                                          @RequestParam(required = false) String contactNumber,
                                          @RequestParam(required = false) String email) {
        List<PatientDto> patient = patientService.findBy(patientId, contactNumber, email);
        if (patient.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error",
                    "No patient found with given criteria."));
        }

        return ResponseEntity.ok(patient);
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get All Patient With Pagination",
            responses = {
                    @ApiResponse(description = "List of patients",
                            responseCode = "200",
                            content = @Content(schema = @Schema(implementation = PatientDto.class))),
                    @ApiResponse(description = "No patients found", responseCode = "404")
            })
    public ResponseEntity<?> getPatientsRecords(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        var patients = patientService.getWithPagination(page, size);
        return patients.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(Map.of("patients", patients.getContent(),
                "currentPage", patients.getNumber(),
                "totalPages", patients.getTotalPages(),
                "totalElements", patients.getTotalElements()));
    }
}

