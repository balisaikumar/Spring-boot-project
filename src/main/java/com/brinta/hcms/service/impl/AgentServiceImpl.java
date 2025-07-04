package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.AgentDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.Agent;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.enums.AgentType;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.exception.exceptionHandler.UnAuthException;
import com.brinta.hcms.mapper.AgentMapper;
import com.brinta.hcms.repository.AgentRepository;
import com.brinta.hcms.repository.DoctorRepository;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterAgentRequest;
import com.brinta.hcms.request.updateRequest.AgentUpdate;
import com.brinta.hcms.service.AgentService;
import com.brinta.hcms.service.JwtService;
import com.brinta.hcms.utility.LoggerUtil;
import com.brinta.hcms.utility.ReferralCodeGenerator;
import com.brinta.hcms.utility.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional
public class AgentServiceImpl implements AgentService {

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ReferralCodeGenerator referralCodeGenerator;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private SecurityUtil securityUtil;

    @Override
    public AgentDto registerAgent(RegisterAgentRequest request) {

        LoggerUtil.info(getClass(), "Registering agent with email: {}", request.getEmail());

        // Prevent EXTERNAL_DOCTOR registration from this endpoint
        if (request.getAgentType() == AgentType.EXTERNAL_DOCTOR) {
            LoggerUtil.warn(getClass(),
                    "Blocked EXTERNAL_DOCTOR registration from agent endpoint");
            throw new UnsupportedOperationException(
                    "External Doctor registration is not allowed through this endpoint. " +
                            "Please use the /api/external-doctors/register endpoint."
            );
        }

        // Check for existing email or contact
        if (agentRepository.existsByEmail(request.getEmail()) ||
                agentRepository.existsByContactNumber(request.getContactNumber())) {
            LoggerUtil.warn(getClass(),
                    "Duplicate agent registration attempt: {}", request.getEmail());
            throw new DuplicateEntryException("Agent with email or contact number already exists.");
        }

        // Generate unique referral code
        String referralCode;
        do {
            referralCode = referralCodeGenerator.generateReferralCode(request.getAgentType(),
                    request.getName());
        } while (agentRepository.existsByAgentCode(referralCode));

        // Encode password and save agent
        Agent agent = Agent.builder()
                .name(request.getName())
                .email(request.getEmail())
                .contactNumber(request.getContactNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .agentType(request.getAgentType())
                .agentCode(referralCode)
                .build();

        Agent savedAgent = agentRepository.save(agent);

        LoggerUtil.info(getClass(), "Agent registered successfully with referralCode: {}",
                referralCode);

        return agentMapper.toDto(savedAgent);
    }

    @Override
    public Map<String, Object> agentLogin(LoginRequest request) {

        LoggerUtil.info(getClass(), "Agent login attempt for email: {}",
                request.getEmail());

        // Commit: [VALIDATION] Basic input checks
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password are required");
        }

        // Commit: [FETCH] Find agent by email
        Agent agent = agentRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    LoggerUtil.warn(getClass(),
                            "Login failed for non-existent agent: {}", request.getEmail());
                    return new UnAuthException("Invalid email or password");
                });

        // Commit: [AUTH] Validate password
        if (!passwordEncoder.matches(request.getPassword(), agent.getPassword())) {
            LoggerUtil.warn(getClass(),
                    "Login failed due to incorrect password for: {}", request.getEmail());
            throw new UnAuthException("Invalid email or password");
        }

        // Commit: [SPRING SECURITY] Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Commit: [JWT] Generate tokens
        TokenPair tokenPair = jwtService.generateTokenPair(authentication);

        LoggerUtil.info(getClass(),
                "Agent logged in successfully: {}", request.getEmail());

        return Map.of(
                "accessToken", tokenPair.getAccessToken(),
                "refreshToken", tokenPair.getRefreshToken()
        );
    }

    @Override
    public Agent updateAgent(Long agentId, AgentUpdate updateRequest) {

        Agent currentAgent = securityUtil.getCurrentAgent();
        if (currentAgent == null) {
            throw new UnAuthException("You must be logged in to update your profile.");
        }

        if (!currentAgent.getId().equals(agentId)) {
            throw new UnAuthException("You are not authorized to update another agent's profile.");
        }

        // Validation checks
        if (updateRequest.getEmail() != null &&
                !updateRequest.getEmail().equals(currentAgent.getEmail()) &&
                agentRepository.existsByEmail(updateRequest.getEmail())) {
            throw new DuplicateEntryException("Email '" +
                    updateRequest.getEmail() + "' is already in use.");
        }

        if (updateRequest.getContactNumber() != null &&
                !updateRequest.getContactNumber()
                        .equals(currentAgent.getContactNumber()) &&
                agentRepository.existsByContactNumber(updateRequest.getContactNumber())) {
            throw new DuplicateEntryException("Contact number '" + updateRequest.getContactNumber() + "' is already in use.");
        }

        // Apply agent updates
        if (updateRequest.getName() != null) currentAgent.setName(updateRequest.getName());
        if (updateRequest.getEmail() != null) currentAgent.setEmail(updateRequest.getEmail());
        if (updateRequest.getContactNumber() != null)
            currentAgent.setContactNumber(updateRequest.getContactNumber());

        Agent updatedAgent = agentRepository.save(currentAgent);

        // --- [NEW] Sync with Doctor if EXTERNAL_DOCTOR ---
        if (updatedAgent.getAgentType() == AgentType.EXTERNAL_DOCTOR) {
            Optional<Doctor> doctorOpt = doctorRepository.findByAgent(updatedAgent);
            if (doctorOpt.isPresent()) {
                Doctor doctor = doctorOpt.get();

                if (updateRequest.getName() != null)
                    doctor.setName(updateRequest.getName());
                if (updateRequest.getEmail() != null)
                    doctor.setEmail(updateRequest.getEmail());
                if (updateRequest.getContactNumber() != null)
                    doctor.setContactNumber(updateRequest.getContactNumber());

                doctorRepository.save(doctor);
                LoggerUtil.info(getClass(),
                        "Synced external doctor data with updated agent ID: {}", agentId);
            } else {
                LoggerUtil.warn(getClass(),
                        "No matching doctor found to sync for agent ID: {}", agentId);
            }
        }

        return updatedAgent;
    }

}

