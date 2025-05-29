package com.brinta.hcms.controller;

import com.brinta.hcms.dto.DoctorProfileDto;
import com.brinta.hcms.entity.DoctorProfile;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.updateRequest.UpdateDoctorRequest;
import com.brinta.hcms.service.DoctorService;
import com.brinta.hcms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/doctor")
@Tag(name = "Doctor Profile API", description = "Operations Related to Doctor")
@AllArgsConstructor
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public ResponseEntity<?> doctorLogin(@RequestBody LoginRequest request) {
        try {
            DoctorProfileDto doctor = userService.doctorLogin(request);
            return ResponseEntity.status(200)
                    .body(Map.of("message", "Login successful", "Doctor", doctor));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).body(ex.getMessage());
        }
    }

    @PutMapping(value = "/update/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update Doctor",
            responses = {
                    @ApiResponse(description = "Parent details updated successfully",
                            responseCode = "200",
                            content = @Content(schema = @Schema(implementation = DoctorProfile.class))),
                    @ApiResponse(description = "Parent not found", responseCode = "404"),
                    @ApiResponse(description = "Invalid input data",
                            responseCode = "400")
            })
    public ResponseEntity<?> updateParent(@PathVariable Long id,
                                          @Valid @RequestBody UpdateDoctorRequest updateDoctorRequest) {
        try {
            DoctorProfile updatedDoctor = doctorService.update(id, updateDoctorRequest);
            return ResponseEntity.ok(Map.of("message",
                    "Parent updated successfully!",
                    "parent", updatedDoctor));
        } catch (ResourceNotFoundException exception) {
            return ResponseEntity.status(404).body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping(value = "/findBy", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Doctor By Parameters",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Doctor found",
                            content = @Content(schema = @Schema(implementation = DoctorProfileDto.class))),
                    @ApiResponse(responseCode = "404",
                            description = "No matching parent found"),
                    @ApiResponse(responseCode = "400",
                            description = "Enter input field")
            })
    public ResponseEntity<?> findByParams(@RequestParam(required = false) Long doctorId,
                                          @RequestParam(required = false) String contactNumber,
                                          @RequestParam(required = false) String email) {
        List<DoctorProfileDto> doctor = doctorService.findBy(doctorId, contactNumber, email);
        if (doctor.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error",
                    "No doctor found with given criteria."));
        }
        return ResponseEntity.ok(doctor);
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get All Doctors With Pagination",
            responses = {
                    @ApiResponse(description = "List of parents",
                            responseCode = "200",
                            content = @Content(schema = @Schema(implementation = DoctorProfileDto.class))),
                    @ApiResponse(description = "No parents found", responseCode = "404")
            })
    public ResponseEntity<?> getParentRecords(@RequestParam(defaultValue = "0") int page,
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
    @Operation(summary = "Delete Doctor",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Doctor Deleted Successfully"),
                    @ApiResponse(responseCode = "400", description = "Enter input field"),
                    @ApiResponse(responseCode = "404", description = "Enter the correct ID")
            })
    public ResponseEntity<?> deleteParentById(@PathVariable("id") Long doctorId) {
        try {
            doctorService.delete(doctorId);
            return ResponseEntity.ok(Map.of("message",
                    "Doctor Deleted successfully!"));
        } catch (ResourceNotFoundException exception) {
            return ResponseEntity.status(404).body(Map.of("error", exception.getMessage()));
        }
    }

}

