package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.DoctorAppointmentDto;
import com.brinta.hcms.dto.DoctorDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.*;
import com.brinta.hcms.enums.AgentType;
import com.brinta.hcms.enums.AppointmentStatus;
import com.brinta.hcms.enums.Roles;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.exception.exceptionHandler.InvalidRequestException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.exception.exceptionHandler.UnAuthException;
import com.brinta.hcms.mapper.DoctorMapper;
import com.brinta.hcms.repository.*;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterDoctorRequest;
import com.brinta.hcms.request.updateRequest.UpdateDoctorRequest;
import com.brinta.hcms.service.DoctorService;
import com.brinta.hcms.service.JwtService;
import com.brinta.hcms.utility.LoggerUtil;
import com.brinta.hcms.utility.ReferralCodeGenerator;

import com.brinta.hcms.utility.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@AllArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private final DoctorMapper doctorMapper;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorAppointmentRepository doctorAppointmentRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private ReferralCodeGenerator referralCodeGenerator;

    @Autowired
    private BranchRepository branchRepository;

    @Override
    public Doctor registerInternalDoctor(RegisterDoctorRequest request) {
        LoggerUtil.info(getClass(), "Attempting internal doctor registration for: {}",
                request.getEmail());

        if (doctorRepository.existsByEmail(request.getEmail()) ||
                doctorRepository.existsByContactNumber(request.getContactNumber())) {
            LoggerUtil.warn(getClass(), "Duplicate internal doctor found: {}",
                    request.getEmail());
            throw new DuplicateEntryException("Email or contact number already exists.");
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUserName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Roles.DOCTOR);
        user.setName(request.getName());
        userRepository.save(user);

        LoggerUtil.debug(getClass(),
                "Internal doctor user created: {}", request.getEmail());

        // Map doctor and save
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + request.getBranchId()));

        Doctor doctor = doctorMapper.register(request, branch);
        doctor.setBranches(Set.of(branch));
        doctor.setUser(user);
        doctor.setReferralCode(null); // Internal doctor doesn’t need referral code
        LoggerUtil.info(getClass(), "Internal doctor saved successfully: {}",
                doctor.getEmail());
        return doctorRepository.save(doctor);
    }

    @Override
    public Doctor registerExternalDoctor(RegisterDoctorRequest request) {

        LoggerUtil.info(getClass(),
                "Attempting external doctor registration for: {}", request.getEmail());

        // Validate agent type
        if (request.getAgentType() == null) {
            LoggerUtil.error(getClass(),
                    "Agent type not provided for external doctor registration");
            throw new ResourceNotFoundException("Agent type is required for external " +
                    "doctor registration");
        }

        // Check duplicates in doctor table
        if (doctorRepository.existsByEmail(request.getEmail()) ||
                doctorRepository.existsByContactNumber(request.getContactNumber())) {
            LoggerUtil.warn(getClass(),
                    "Duplicate external doctor found: {}", request.getEmail());
            throw new DuplicateEntryException("Email or contact number already exists.");
        }

        // Check duplicates in agent table
        if (agentRepository.existsByEmail(request.getEmail()) ||
                agentRepository.existsByContactNumber(request.getContactNumber())) {
            LoggerUtil.warn(getClass(), "Duplicate agent found: {}", request.getEmail());
            throw new DuplicateEntryException("Agent with given email or contact number " +
                    "already exists.");
        }

        // Generate unique referral code
        String referralCode;
        do {
            referralCode = referralCodeGenerator
                    .generateReferralCode(request.getAgentType(), request.getName());
        } while (agentRepository.existsByAgentCode(referralCode));

        LoggerUtil.info(getClass(), "Generated referral code: {}", referralCode);

        // Save to Agent table
        Agent agent = Agent.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .contactNumber(request.getContactNumber())
                .agentCode(referralCode)
                .agentType(request.getAgentType())
                .build();

        Agent savedAgent = agentRepository.save(agent); // Save agent and keep reference
        LoggerUtil.info(getClass(),
                "Agent saved for external doctor: {}", request.getEmail());

        // Save to Doctor table (linked to saved agent)
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + request.getBranchId()));

        Doctor doctor = doctorMapper.register(request, branch);
        doctor.setBranches(Set.of(branch));
        doctor.setReferralCode(referralCode);             // Set referral
        doctor.setAgent(savedAgent);                      // Link agent to doctor here
        Doctor savedDoctor = doctorRepository.save(doctor);

        LoggerUtil.info(getClass(),
                "External doctor saved successfully: {}", savedDoctor.getEmail());
        return savedDoctor;
    }

    @Override
    public Map<String, Object> doctorLogin(LoginRequest request) {
        LoggerUtil.info(getClass(), "Doctor login attempt for email: {}",
                request.getEmail());

        // Validate request fields
        if (request.getEmail() == null || request.getPassword() == null) {
            LoggerUtil.warn(getClass(), "Missing email or password in login request");
            throw new IllegalArgumentException("Email and password must be provided");
        }

        // Attempt Internal Doctor Login (via User entity)
        Optional<Doctor> internalDoctorOpt = doctorRepository.findByEmail(request.getEmail());

        if (internalDoctorOpt.isPresent()) {
            Doctor internalDoctor = internalDoctorOpt.get();

            if (internalDoctor.getUser() != null) {
                String encodedPassword = internalDoctor.getUser().getPassword();

                // Validate password
                if (!passwordEncoder.matches(request.getPassword(), encodedPassword)) {
                    LoggerUtil.warn(getClass(),

                            "Invalid password for internal doctor: {}", request.getEmail());
                    throw new RuntimeException("Invalid credentials for internal doctor");
                }

                // Authenticate using Spring Security
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(request.getEmail(),
                                request.getPassword())
                );

                TokenPair tokenPair = jwtService.generateTokenPair(authentication);
                LoggerUtil.info(getClass(), "Internal doctor login successful for: {}",
                        request.getEmail());

                return Map.of(
                        "accessToken", tokenPair.getAccessToken(),
                        "refreshToken", tokenPair.getRefreshToken()
                );
            }
        }

        // Attempt External Doctor Login (via Agent entity)
        Optional<Agent> externalDoctorOpt = agentRepository.findByEmail(request.getEmail());

        if (externalDoctorOpt.isPresent()) {
            Agent agent = externalDoctorOpt.get();

            if (agent.getAgentType() == AgentType.EXTERNAL_DOCTOR) {
                if (!passwordEncoder.matches(request.getPassword(), agent.getPassword())) {
                    LoggerUtil.warn(getClass(), "Invalid password for external doctor: {}",
                            request.getEmail());
                    throw new RuntimeException("Invalid credentials for external doctor");
                }

                // Authenticate for token generation (even if no UserDetails object is used)
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(request.getEmail(),
                                request.getPassword())
                );

                TokenPair tokenPair = jwtService.generateTokenPair(authentication);
                LoggerUtil.info(getClass(), "External doctor login successful for: {}",
                        request.getEmail());

                return Map.of(
                        "accessToken", tokenPair.getAccessToken(),
                        "refreshToken", tokenPair.getRefreshToken()
                );
            }
        }

        // If no doctor found in either repository
        LoggerUtil.error(getClass(), "Doctor login failed for email: {}",
                request.getEmail());
        throw new ResourceNotFoundException("Doctor not found or invalid credentials");

    }

    @Override
    public Doctor update(Long doctorId, UpdateDoctorRequest updateDoctorRequest) {

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new
                        ResourceNotFoundException("Doctor with ID " + doctorId + " not found."));

        // Ensure the doctor is internal (i.e., has a user)
        if (doctor.getUser() == null) {
            LoggerUtil.warn(getClass(),
                    "Attempted to update external doctor [{}] via internal doctor " +
                            "update API.", doctorId);
            throw new UnAuthException("External doctors cannot be updated via this route. " +
                    "Use agent update instead.");
        }

        User user = doctor.getUser();

        // Update user fields
        if (updateDoctorRequest.getName() != null)
            user.setName(updateDoctorRequest.getName());
        if (updateDoctorRequest.getEmail() != null)
            user.setEmail(updateDoctorRequest.getEmail());

        userRepository.save(user);

        // Update doctor fields
        doctorMapper.update(updateDoctorRequest, doctor);
        doctor.setUser(user); // Ensure user is re-set in case of object refresh

        Doctor updatedDoctor = doctorRepository.save(doctor);

        // Handle multiple branch IDs
        if (updateDoctorRequest.getBranchIds() != null && !updateDoctorRequest.getBranchIds().isEmpty()) {
            Set<Branch> branches = updateDoctorRequest.getBranchIds().stream()
                    .map(id -> branchRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + id)))
                    .collect(Collectors.toSet());
            doctor.setBranches(branches); // <- assign multiple branches
        }

        LoggerUtil.info(getClass(),
                "Internal doctor [{}] updated successfully by admin.", doctorId);
        return updatedDoctor;
    }

    @Override
    public List<DoctorDto> findBy(Long doctorId, String contactNumber, String email) {
        String maskedContact = LoggerUtil.mask(contactNumber);
        String maskedEmail = LoggerUtil.mask(email);

        log.info("Doctor search attempt with doctorId={}, contact={}, email={}",
                doctorId, maskedContact, maskedEmail);

        if (doctorId == null &&
                (contactNumber == null || contactNumber.isEmpty()) &&
                (email == null || email.isEmpty())) {
            log.warn("Doctor search failed: all input fields are empty or null");
            throw new ResourceNotFoundException("Enter Correct Input");
        }

        Optional<Doctor> doctor = doctorRepository.findByIdOrContactNumberOrEmail(doctorId, contactNumber, email);

        if (doctor.isEmpty()) {
            log.warn("Doctor search failed: no match found for doctorId={}, contact={}, email={}",
                    doctorId, maskedContact, maskedEmail);
            throw new ResourceNotFoundException("No Matching Doctor found in Database");
        }

        log.info("Doctor found successfully for search: doctorId={}, contact={}, email={}",
                doctorId, maskedContact, maskedEmail);

        return doctor.stream()
                .map(doctorMapper::findBy)
                .collect(Collectors.toList());
    }

    @Override
    public Page<DoctorDto> getWithPagination(int page, int size) {
        log.info("Pagination request: page={}, size={}", page, size);

        if (page < 0 || size <= 0) {
            log.warn("Invalid pagination parameters: page={}, size={}", page, size);
            throw new InvalidRequestException("Page index must not be negative and size must be greater than zero.");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Doctor> doctorPage = doctorRepository.findAll(pageable);

        if (doctorPage.isEmpty()) {
            log.info("Pagination result: No doctors found for page={}, size={}", page, size);
            return Page.empty();
        }

        log.info("Pagination success: page={}, size={}, totalElements={}",
                page, size, doctorPage.getTotalElements());

        return doctorPage.map(doctorMapper::toDto);
    }

    @Override
    public void delete(Long doctorId) {

        // Get the current authenticated actor
        Object actor = securityUtil.getCurrentActor();

        if (actor instanceof Agent agent && agent.getAgentType() == AgentType.EXTERNAL_DOCTOR) {
            LoggerUtil.warn(getClass(),
                    "Unauthorized delete attempt by external doctor agent with email: {}",
                    LoggerUtil.mask(agent.getEmail()));
            throw new UnAuthException("External doctors cannot delete profile via this route. " +
                    "Please contact the admin or use appropriate support.");
        }

        if (actor instanceof User currentUser) {
            String maskedEmail = LoggerUtil.mask(currentUser.getEmail());
            LoggerUtil.info(getClass(), "Doctor delete attempt by user={} for doctorId={}",
                    maskedEmail, doctorId);

            Doctor currentDoctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Doctor profile not found for current user"));

            if (!currentDoctor.getId().equals(doctorId)) {
                LoggerUtil.warn(getClass(),
                        "Unauthorized delete attempt: user={} tried to delete doctorId={}",
                        maskedEmail, doctorId);
                throw new UnAuthException("You are not authorized to delete this doctor profile.");
            }

            // Delete any linked agent (optional cleanup)
            agentRepository.findByEmail(currentUser.getEmail()).ifPresent(agent -> {
                agentRepository.delete(agent);
                LoggerUtil.debug(getClass(),
                        "Linked agent deleted for internal doctor: {}", maskedEmail);
            });

            doctorRepository.delete(currentDoctor); // First delete doctor
            userRepository.delete(currentUser);     // Then delete linked user

            LoggerUtil.info(getClass(),
                    "Internal doctor with ID [{}] and user [{}] deleted successfully.",
                    doctorId, maskedEmail);

        } else {
            LoggerUtil.warn(getClass(),
                    "Unauthorized actor tried to delete doctor profile.");
            throw new
                    UnAuthException("Unauthorized access. Only internal doctors can delete " +
                    "this profile.");
        }
    }

    @Override
    public Page<DoctorAppointmentDto> listAppointments(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new
                    InvalidRequestException("Page index must not be negative and size must be " +
                    "greater than zero.");
        }

        Long userId = securityUtil.getCurrentUser().getId();
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new
                        ResourceNotFoundException("Doctor not found for the logged-in user"));

        Pageable pageable = PageRequest.of(page, size);
        Page<DoctorAppointment> appointmentPage = doctorAppointmentRepository
                .findByDoctorId(doctor.getId(), pageable);

        return appointmentPage.map(doctorMapper::toDto);
    }

    @Override
    public DoctorAppointmentDto rescheduleAppointment(Long appointmentId, LocalDateTime newTime) {
        Long userId = securityUtil.getCurrentUser().getId();

        // Fetch the doctor based on userId
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new
                        ResourceNotFoundException("Doctor not found for logged-in user"));

        // Fetch the appointment using the appointmentId
        DoctorAppointment doctorAppointment = doctorAppointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("DoctorAppointment not found"));

        if (!doctorAppointment.getDoctor().getId().equals(doctor.getId())) {
            throw new
                    RuntimeException("Unauthorized: You can only reschedule your own appointments");
        }

        doctorAppointment.setAppointmentTime(newTime);
        doctorAppointment.setStatus(AppointmentStatus.RESCHEDULED);

        return doctorMapper.toDto(doctorAppointmentRepository.save(doctorAppointment));
    }

    @Override
    public void cancelAppointment(Long appointmentId) {
        Long userId = securityUtil.getCurrentUser().getId();

        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new
                        ResourceNotFoundException("Doctor not found for logged-in user"));

        DoctorAppointment doctorAppointment = doctorAppointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("DoctorAppointment not found"));

        if (!doctorAppointment.getDoctor().getId().equals(doctor.getId())) {
            throw new RuntimeException("Unauthorized: You can only cancel your own appointments");
        }

        doctorAppointment.setStatus(AppointmentStatus.CANCELLED);
        doctorAppointmentRepository.save(doctorAppointment);
    }

}

