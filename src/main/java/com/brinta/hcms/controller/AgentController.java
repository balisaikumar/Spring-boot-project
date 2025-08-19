package com.brinta.hcms.controller;

import com.brinta.hcms.dto.AgentDto;
import com.brinta.hcms.entity.Agent;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.exception.exceptionHandler.UnAuthException;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterAgentRequest;
import com.brinta.hcms.request.updateRequest.AgentUpdate;
import com.brinta.hcms.service.AgentService;
import com.brinta.hcms.utility.LoggerUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    // [ADMIN ONLY] Register new Agent
    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Register a new Agent",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Agent registered successfully",
                            content = @Content(schema = @Schema(implementation = AgentDto.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Agent already exists")
            }
    )
    public ResponseEntity<?> registerAgent(@Valid @RequestBody RegisterAgentRequest request) {
        LoggerUtil.info(getClass(), "Registering agent with email: {}", request.getEmail());
        try {
            AgentDto agentDto = agentService.registerAgent(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Agent registered successfully",
                    "agent", agentDto
            ));
        } catch (DuplicateEntryException ex) {
            LoggerUtil.warn(getClass(), "Duplicate agent registration: {}",
                    request.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        } catch (UnsupportedOperationException ex) {
            LoggerUtil.warn(getClass(), "Blocked unsupported agent registration: {}",
                    ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception e) {
            LoggerUtil.error(getClass(), "Unexpected error while registering agent", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal Server Error"));
        }
    }

    // [AGENT LOGIN] Handles agent login
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Login as an Agent",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login successful"),
                    @ApiResponse(responseCode = "400", description = "Missing or invalid input"),
                    @ApiResponse(responseCode = "401", description = "Invalid email or password")
            }
    )
    public ResponseEntity<?> loginAgent(@Valid @RequestBody LoginRequest request) {
        LoggerUtil.info(getClass(), "Agent login attempt: {}", request.getEmail());
        try {
            return ResponseEntity.ok(agentService.agentLogin(request));
        } catch (IllegalArgumentException ex) {
            LoggerUtil.warn(getClass(), "Invalid login input: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (UnAuthException ex) {
            LoggerUtil.warn(getClass(), "Login failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error",
                    ex.getMessage()));
        } catch (Exception e) {
            LoggerUtil.error(getClass(), "Unexpected error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal Server Error"));
        }
    }

    // [SELF UPDATE] Agents can update their own profile
    @PutMapping(value = "/update/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('AGENT')")
    @Operation(
            summary = "Update your own agent profile",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Agent updated successfully",
                            content = @Content(schema = @Schema(implementation = AgentDto.class))),
                    @ApiResponse(responseCode = "403", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Agent not found"),
                    @ApiResponse(responseCode = "400", description = "Validation error")
            }
    )
    public ResponseEntity<?> updateAgentProfile(@PathVariable Long id,
                                                @Valid @RequestBody AgentUpdate updateRequest) {
        LoggerUtil.info(getClass(), "Agent update request for ID: {}", id);
        try {
            Agent updatedAgent = agentService.updateAgent(id, updateRequest);
            return ResponseEntity.ok(Map.of(
                    "message", "Agent profile updated successfully!",
                    "agent", updatedAgent
            ));
        } catch (UnAuthException ex) {
            LoggerUtil.warn(getClass(), "Unauthorized update attempt for agent ID: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", ex.getMessage()));
        } catch (ResourceNotFoundException ex) {
            LoggerUtil.warn(getClass(), "Agent not found for ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        } catch (DuplicateEntryException ex) {
            LoggerUtil.warn(getClass(), "Duplicate update input: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            LoggerUtil.error(getClass(), "Unexpected error during agent update", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

}

