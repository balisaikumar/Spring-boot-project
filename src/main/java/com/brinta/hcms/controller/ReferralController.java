package com.brinta.hcms.controller;

import com.brinta.hcms.dto.ReferralDto;
import com.brinta.hcms.enums.AgentType;
import com.brinta.hcms.enums.ProfileStatus;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.exception.exceptionHandler.InvalidRequestException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.exception.exceptionHandler.UnAuthException;
import com.brinta.hcms.request.registerRequest.ReferralRequest;
import com.brinta.hcms.service.ReferralService;
import com.brinta.hcms.utility.LoggerUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/referral")
@RequiredArgsConstructor
public class ReferralController {

    private static final Class<?> logger = ReferralController.class;

    private final ReferralService referralService;

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('AGENT')")
    @Operation(
            summary = "Create a patient referral",
            description = "Allows only authenticated agents (of any type) to refer a patient. " +
                    "Throws error if patient is already referred.",
            responses = {
                    @ApiResponse(responseCode = "201",
                            description = "Referral created successfully",
                            content = @Content(schema =
                            @Schema(implementation = ReferralDto.class))),
                    @ApiResponse(responseCode = "400",
                            description = "Patient already referred or invalid data"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access"),
                    @ApiResponse(responseCode = "404", description = "Doctor or agent not found"),
                    @ApiResponse(responseCode = "500", description = "Server error")
            }
    )
    public ResponseEntity<?> createReferral(@Valid @RequestBody ReferralRequest referralRequest) {
        LoggerUtil.info(logger,
                "Received referral creation request for patient contact: {}",
                referralRequest.getPatientContactNumber());
        try {
            ReferralDto referral = referralService.createReferral(referralRequest);
            return ResponseEntity.status(201).body(Map.of(
                    "message", "Referral created successfully!",
                    "referral", referral
            ));
        } catch (DuplicateEntryException | UnAuthException | InvalidRequestException ex) {
            LoggerUtil.warn(logger, "Referral creation failed: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (ResourceNotFoundException ex) {
            LoggerUtil.warn(logger, "Doctor or agent not found: {}", ex.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            LoggerUtil.error(logger, "Unexpected error during referral creation", ex);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Something went wrong"));
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get referral by ID",
            description = "Fetch a single referral's details by its ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Referral found"),
                    @ApiResponse(responseCode = "404", description = "Referral not found"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access")
            }
    )
    public ResponseEntity<?> getReferralById(@PathVariable("id") Long id) {
        try {
            ReferralDto referral = referralService.getReferralById(id);
            return ResponseEntity.ok(referral);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error"));
        }
    }

    @GetMapping(value = "/my-referrals", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('AGENT')")
    @Operation(
            summary = "Get current agent's referrals",
            description = "Fetches all referrals made by the currently logged-in agent",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Referrals retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access")
            }
    )
    public ResponseEntity<?> getMyReferrals() {
        try {
            List<ReferralDto> referrals = referralService.getReferralsForCurrentAgent();
            return ResponseEntity.ok(referrals);
        } catch (Exception ex) {
            LoggerUtil.error(logger, "Failed to fetch referrals for current agent", ex);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Something went wrong"));
        }
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(
            summary = "Get all referrals (paginated)",
            description = "Returns a paginated list of all referrals",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Referrals fetched successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access")
            }
    )
    public ResponseEntity<?> getAllReferrals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ReferralDto> referrals = referralService.getAllReferralsWithPagination(page, size);
        return ResponseEntity.ok(referrals);
    }

    @GetMapping(value = "/by-agent-type/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Get referrals by agent type",
            description = "Returns all referrals made by agents of a " +
                    "specific type (e.g., PHARMACY, EXTERNAL_DOCTOR)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Referrals fetched successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid AgentType"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access")
            }
    )
    public ResponseEntity<?> getReferralsByAgentType(@PathVariable("type") String type) {
        try {
            AgentType agentType = AgentType.valueOf(type.toUpperCase());
            List<ReferralDto> referrals = referralService.getReferralsByAgentType(agentType);
            return ResponseEntity.ok(referrals);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid AgentType: " + type));
        }
    }

    @GetMapping("/referrals/status")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(
            summary = "Get referrals by profile status",
            description = "Fetches paginated referrals based on their profile status. " +
                    "Accessible by AGENT and ADMIN roles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Referrals fetched successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ReferralDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access")
            }
    )
    public ResponseEntity<?> getReferralsByStatus(
            @RequestParam ProfileStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            LoggerUtil.info(logger, "Fetching referrals with status: {}", status);
            Page<ReferralDto> response = referralService
                    .getReferralsByProfileStatus(status, page, size);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            LoggerUtil.warn(logger, "Invalid status input: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid profile status"));
        } catch (Exception e) {
            LoggerUtil.error(logger, "Failed to fetch referrals by status", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Something went wrong"));
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<?> deleteReferral(@PathVariable Long id) {
        try {
            referralService.deleteReferralById(id);
            return ResponseEntity.ok(Map.of("message", "Referral deleted successfully"));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Something went wrong"));
        }
    }

}

