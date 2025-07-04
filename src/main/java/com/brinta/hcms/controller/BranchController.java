package com.brinta.hcms.controller;

import com.brinta.hcms.entity.Branch;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.exception.exceptionHandler.InvalidRequestException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.request.registerRequest.RegisterBranchRequest;
import com.brinta.hcms.request.updateRequest.UpdateBranchRequest;
import com.brinta.hcms.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/branch")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new Branch",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Branch created",
                            content = @Content(schema = @Schema(implementation = Branch.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input")
            })
    public ResponseEntity<?> createBranch(@Valid @RequestBody RegisterBranchRequest request) {
        try {
            Branch savedBranch = branchService.registerBranch(request);
            return ResponseEntity.status(201).
                    body(Map.of("message","Branch created successfully","branch",savedBranch));
        } catch (DuplicateEntryException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Something went wrong"));
        }
    }

    @GetMapping(value = "/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Get all branches with pagination")
    public ResponseEntity<?> getAllBranches(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Branch> branchPage = branchService.getAllBranches(page, size);
            return ResponseEntity.ok(Map.of(
                    "branches", branchPage.getContent(),
                    "totalPages", branchPage.getTotalPages(),
                    "totalElements", branchPage.getTotalElements(),
                    "currentPage", branchPage.getNumber()
            ));
        } catch (InvalidRequestException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/findBy", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Find Branch by ID, Code or Name")
    public ResponseEntity<?> findByParams(@RequestParam(required = false) Long branchId,
                                          @RequestParam(required = false) String branchCode,
                                          @RequestParam(required = false) String branchName) {
        try {
            List<Branch> branches = branchService.findBy(branchId, branchCode, branchName);
            if (branches.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "No branch found with given criteria."));
            }
            return ResponseEntity.ok(branches);
        } catch (InvalidRequestException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping(value = "/update/{id}",produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Update Branch Details", responses = {
            @ApiResponse(responseCode = "200", description = "Branch updated successfully"),
            @ApiResponse(responseCode = "404", description = "Branch not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<?> updateBranch(@PathVariable Long id,
                                          @RequestBody UpdateBranchRequest request) {
        try {
            Branch updatedBranch = branchService.updateBranch(id, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Branch updated successfully",
                    "branch", updatedBranch
            ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(Map.of("error", "Failed to update branch: " + e.getMessage()));
        }
    }

    @DeleteMapping(value = "delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Branch by ID", responses = {
            @ApiResponse(responseCode = "200", description = "Branch deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    public ResponseEntity<?> deleteBranch(@PathVariable Long id) {
        try {
            branchService.deleteBranch(id);
            return ResponseEntity.ok(Map.of("message", "Branch deleted successfully"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

}