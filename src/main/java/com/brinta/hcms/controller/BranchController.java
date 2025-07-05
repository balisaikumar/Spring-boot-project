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
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/branch")
@AllArgsConstructor
@NoArgsConstructor
public class BranchController {

    @Autowired
    private BranchService branchService;

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new Branch",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Branch created",
                            content = @Content(schema = @Schema(implementation = Branch.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input")
            })
    public ResponseEntity<?> createBranch(@Valid @RequestBody RegisterBranchRequest request) {
        log.info("Received request to create branch with name={}, code={}",
                request.getBranchName(), request.getBranchCode());
        try {
            Branch savedBranch = branchService.registerBranch(request);
            log.info("Branch created successfully with ID={}", savedBranch.getId());
            return ResponseEntity.status(201).body(Map.of(
                    "message", "Branch created successfully",
                    "branch", savedBranch
            ));
        } catch (DuplicateEntryException e) {
            log.warn("Failed to create branch due to duplicate entry: {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while creating branch: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Something went wrong"));
        }
    }

    @GetMapping(value = "/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Get all branches with pagination")
    public ResponseEntity<?> getAllBranches(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        log.info("Received request to fetch all branches with page={}, size={}", page, size);
        try {
            Page<Branch> branchPage = branchService.getAllBranches(page, size);
            log.info("Fetched {} branches on page {}", branchPage.getNumberOfElements(),
                    branchPage.getNumber());
            return ResponseEntity.ok(Map.of(
                    "branches", branchPage.getContent(),
                    "totalPages", branchPage.getTotalPages(),
                    "totalElements", branchPage.getTotalElements(),
                    "currentPage", branchPage.getNumber()
            ));
        } catch (InvalidRequestException e) {
            log.warn("Invalid request while fetching branches: {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/findBy", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Find Branch by ID, Code or Name")
    public ResponseEntity<?> findByParams(@RequestParam(required = false) Long branchId,
                                          @RequestParam(required = false) String branchCode,
                                          @RequestParam(required = false) String branchName) {
        log.info("Received request to find branch by branchId={}, branchCode={}, branchName={}",
                branchId, branchCode, branchName);
        try {
            List<Branch> branches = branchService.findBy(branchId, branchCode, branchName);
            if (branches.isEmpty()) {
                log.warn("No branches found for provided criteria.");
                return ResponseEntity.status(404).body(Map.of("error", "No branch found with given criteria."));
            }
            log.info("Found {} branch(es) matching criteria.", branches.size());
            return ResponseEntity.ok(branches);
        } catch (InvalidRequestException e) {
            log.warn("Invalid request parameters while finding branch: {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping(value = "/update/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
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
            log.info("Branch updated successfully: ID={}", updatedBranch.getId());
            return ResponseEntity.ok(Map.of(
                    "message", "Branch updated successfully",
                    "branch", updatedBranch
            ));
        } catch (ResourceNotFoundException e) {
            log.warn("Branch with ID={} not found: {}", id, e.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update branch with ID={}: {}", id, e.getMessage(), e);
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
        log.info("Received request to delete branch with ID={}", id);
        try {
            branchService.deleteBranch(id);
            log.info("Branch deleted successfully with ID={}", id);
            return ResponseEntity.ok(Map.of("message", "Branch deleted successfully"));
        } catch (ResourceNotFoundException e) {
            log.warn("Branch with ID={} not found for deletion: {}", id, e.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

}

