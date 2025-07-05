package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.ReferralDto;
import com.brinta.hcms.entity.Agent;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.entity.Patient;
import com.brinta.hcms.entity.Referral;
import com.brinta.hcms.enums.AgentType;
import com.brinta.hcms.enums.PatientRegistrationStatus;
import com.brinta.hcms.enums.ProfileStatus;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.exception.exceptionHandler.NoReferralFoundException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.exception.exceptionHandler.UnAuthException;
import com.brinta.hcms.mapper.PatientMapper;
import com.brinta.hcms.mapper.ReferralMapper;
import com.brinta.hcms.repository.DoctorRepository;
import com.brinta.hcms.repository.PatientRepository;
import com.brinta.hcms.repository.ReferralRepository;
import com.brinta.hcms.request.registerRequest.ReferralRequest;
import com.brinta.hcms.service.ReferralService;
import com.brinta.hcms.utility.LoggerUtil;
import com.brinta.hcms.utility.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReferralServiceImpl implements ReferralService {

    private static final Class<?> logger = ReferralServiceImpl.class;

    @Autowired
    private ReferralRepository referralRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientMapper patientMapper;

    @Autowired
    private ReferralMapper referralMapper;

    @Autowired
    private SecurityUtil securityUtil;

    @Override
    public ReferralDto createReferral(ReferralRequest request) {

        Agent currentAgent = securityUtil.getCurrentAgent();

        if (currentAgent.getAgentType() == null) {
            LoggerUtil.warn(logger,
                    "Unauthorized: Agent '{}' has no agent type", currentAgent.getEmail());
            throw new UnAuthException("Agent type is not defined for this agent.");
        }

        if (currentAgent.getAgentType().equals(AgentType.EXTERNAL_DOCTOR)) {

            // Validate External Doctor identity
            if (!currentAgent.getName().equalsIgnoreCase(request.getDoctorName()) ||
                    !currentAgent.getEmail().equalsIgnoreCase(request.getDoctorEmail()) ||
                    !currentAgent.getContactNumber().equals(request.getDoctorContactNumber())) {

                LoggerUtil.warn(logger, "Doctor details mismatch with token");
                throw new UnAuthException("Doctor details mismatch with your profile");
            }

            // Must already exist
            Doctor doctor = doctorRepository.findByEmailAndContactNumber(
                    request.getDoctorEmail(), request.getDoctorContactNumber()
            ).orElseThrow(() -> {
                LoggerUtil.warn(logger,
                        "Doctor not found in DB: {}", request.getDoctorEmail());
                return new ResourceNotFoundException("Doctor not registered in the system.");
            });

            return processReferral(currentAgent, doctor, request);

        } else {
            // Validate normal agent identity
            if (!currentAgent.getName().equalsIgnoreCase(request.getName()) ||
                    !currentAgent.getEmail().equalsIgnoreCase(request.getEmail()) ||
                    !currentAgent.getContactNumber().equals(request.getContactNumber())) {

                LoggerUtil.warn(logger, "Agent details mismatch with token");
                throw new UnAuthException("Agent details mismatch with your profile");
            }

            return processReferral(currentAgent, null, request);
        }
    }

    /**
     * [COMMIT] Common referral creation logic for both external doctors and other agents
     */
    private ReferralDto processReferral(Agent agent, Doctor doctor, ReferralRequest request) {
        Patient patient = patientRepository.findByContactNumber(request.getPatientContactNumber())
                .orElse(null);

        if (patient != null) {
            if (referralRepository.existsByAgentAndPatient(agent, patient)) {
                LoggerUtil.warn(logger,
                        "Duplicate referral attempt by agent [{}] for patient [{}]",
                        agent.getId(), patient.getContactNumber());
                throw new DuplicateEntryException("This patient has already been referred by you.");
            }

            if (patient.getStatus() == null)
                patient.setStatus(PatientRegistrationStatus.REFERRAL);
            if (patient.getProfileStatus() == null)
                patient.setProfileStatus(ProfileStatus.PENDING);

            patientRepository.save(patient);
        } else {
            patient = patientMapper.fromReferralRequest(request);
            patient.setStatus(PatientRegistrationStatus.REFERRAL);
            patient.setProfileStatus(ProfileStatus.PENDING);
            patientRepository.save(patient);

            LoggerUtil.info(logger, "New patient [{}] created by agent [{}]",
                    patient.getContactNumber(), agent.getId());
        }

        Referral referral = new Referral();
        referral.setDoctor(doctor);
        referral.setPatient(patient);
        referral.setAgent(agent);
        referral.setArea(request.getArea());
        referral.setProfileStatus(
                patient.getUser() != null ? ProfileStatus.COMPLETED : ProfileStatus.PENDING
        );

        Referral savedReferral = referralRepository.save(referral);
        LoggerUtil.info(logger, "Referral [{}] created by agent [{}]",
                savedReferral.getId(), agent.getId());

        // Use enhanced mapper method that nullifies agentName for EXTERNAL_DOCTOR
        ReferralDto dto = referralMapper
                .createReferralWithAgentType(savedReferral, agent.getAgentType());
        dto.setProfileStatus(referral.getProfileStatus());
        return dto;
    }

    @Override
    public ReferralDto getReferralById(Long referralId) {
        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> new
                        ResourceNotFoundException("Referral not found with ID: " + referralId));

        return referralMapper.createReferral(referral);
    }

    @Override
    public List<ReferralDto> getReferralsForCurrentAgent() {
        Agent currentAgent = securityUtil.getCurrentAgent();

        List<Referral> referrals = referralRepository.findByAgent(currentAgent);

        return referrals.stream()
                .map(referralMapper::createReferral)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ReferralDto> getAllReferralsWithPagination(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Referral> referralPage = referralRepository.findAll(pageable);

        if (referralPage.isEmpty()) {
            throw new NoReferralFoundException("No referrals found.");
        }

        return referralPage.map(referralMapper::createReferral); // Convert each Referral to ReferralDto
    }

    @Override
    public List<ReferralDto> getReferralsByAgentType(AgentType agentType) {
        List<Referral> referrals = referralRepository.findByAgent_AgentType(agentType);

        if (referrals.isEmpty()) {
            throw new
                    NoReferralFoundException("No referrals found for agent type: " + agentType.name());
        }

        return referrals.stream()
                .map(referralMapper::createReferral)
                .toList();
    }

    @Override
    public Page<ReferralDto> getReferralsByProfileStatus(ProfileStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Referral> referrals = referralRepository.findByProfileStatus(status, pageable);

        if (referrals.isEmpty()) {
            throw new
                    NoReferralFoundException("No referrals found with profile status: "
                    + status.name());
        }

        List<ReferralDto> referralDtoList = referralMapper.createReferralList(referrals.getContent());

        return new PageImpl<>(referralDtoList, pageable, referrals.getTotalElements());
    }

    @Override
    public void deleteReferralById(Long referralId) {
        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> new
                        ResourceNotFoundException("Referral not found with ID: " + referralId));

        referralRepository.delete(referral);
        LoggerUtil.info(logger, "Referral with ID [{}] deleted successfully", referralId);
    }

}

