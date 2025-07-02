package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.DoctorAppointmentDto;
import com.brinta.hcms.dto.DoctorDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.entity.DoctorAppointment;
import com.brinta.hcms.entity.User;
import com.brinta.hcms.enums.AppointmentStatus;
import com.brinta.hcms.enums.Roles;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.exception.exceptionHandler.InvalidRequestException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.exception.exceptionHandler.UnAuthException;
import com.brinta.hcms.mapper.DoctorMapper;
import com.brinta.hcms.repository.DoctorAppointmentRepository;
import com.brinta.hcms.repository.DoctorRepository;
import com.brinta.hcms.repository.UserRepository;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterDoctorRequest;
import com.brinta.hcms.request.updateRequest.UpdateDoctorRequest;
import com.brinta.hcms.service.DoctorService;
import com.brinta.hcms.service.JwtService;
import com.brinta.hcms.utility.LoggerUtil;
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
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SecurityUtil securityUtil;

    @Override
    public Doctor register(RegisterDoctorRequest registerDoctor) {
        String maskedEmail = LoggerUtil.mask(registerDoctor.getEmail());
        String maskedContact = LoggerUtil.mask(registerDoctor.getContactNumber());

        log.info("Attempting to register doctor with email={}, contact={}",
                maskedEmail, maskedContact);

        boolean emailExists = doctorRepository.existsByEmail(registerDoctor.getEmail());
        boolean contactExists = doctorRepository.existsByContactNumber(registerDoctor.getContactNumber());

        if (emailExists || contactExists) {
            StringBuilder errorMessage = new StringBuilder("Already Exists: ");
            if (emailExists) errorMessage.append("Email: ").append(maskedEmail);
            if (emailExists && contactExists) errorMessage.append(" | ");
            if (contactExists) errorMessage.append("Contact: ").append(maskedContact);

            log.warn("Doctor registration failed due to duplicate: {}", errorMessage);
            throw new DuplicateEntryException(errorMessage.toString());
        }

        User saveUser = new User();
        saveUser.setUsername(registerDoctor.getUserName());
        saveUser.setName(registerDoctor.getName());
        saveUser.setEmail(registerDoctor.getEmail());
        saveUser.setPassword(passwordEncoder.encode(registerDoctor.getPassword())); // DO NOT log
        saveUser.setRole(Roles.DOCTOR);

        Doctor doctor = doctorMapper.register(registerDoctor);
        doctor.setUser(saveUser);
        saveUser.setDoctor(doctor);

        userRepository.save(saveUser);

        log.info("Doctor registered successfully: username={}, email={}, contact={}",
                registerDoctor.getUserName(), maskedEmail, maskedContact);

        return doctor;
    }

    @Override
    public Map<String, Object> doctorLogin(LoginRequest request) {
        String maskedEmail = LoggerUtil.mask(request.getEmail());

        // Validate input
        if (request.getEmail() == null || request.getPassword() == null) {
            log.warn("Doctor login failed: missing email or password.");
            throw new IllegalArgumentException("Email and password must be provided");
        }

        log.info("Doctor login attempt for email={}", maskedEmail);

        // Fetch the doctor by email
        Doctor doctor = doctorRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Doctor login failed: no doctor found with email={}", maskedEmail);
                    return new RuntimeException("Doctor not found with email: " + maskedEmail);
                });

        // Check if user credentials are properly set
        if (doctor.getUser() == null || doctor.getUser().getPassword() == null) {
            log.error("Doctor login failed: user credentials missing for email={}", maskedEmail);
            throw new RuntimeException("User credentials are not set properly for doctor: " + maskedEmail);
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), doctor.getUser().getPassword())) {
            log.warn("Doctor login failed: invalid password for email={}", maskedEmail);
            throw new RuntimeException("Invalid password");
        }

        // Authenticate using Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        doctor.getUser().getEmail(),
                        request.getPassword()
                )
        );

        // Generate JWT token pair
        TokenPair tokenPair = jwtService.generateTokenPair(authentication);

        log.info("Doctor logged in successfully: email={}", maskedEmail);

        // Return tokens (do NOT log tokens)
        return Map.of(
                "accessToken", tokenPair.getAccessToken(),
                "refreshToken", tokenPair.getRefreshToken()
        );
    }

    @Override
    public Doctor update(Long doctorId, UpdateDoctorRequest updateDoctorRequest) {
        User currentUser = securityUtil.getCurrentUser();
        String maskedEmail = LoggerUtil.mask(currentUser.getEmail());

        log.info("Doctor update attempt by user={}", maskedEmail);

        Doctor currentDoctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> {
                    log.warn("Doctor update failed: profile not found for user={}", maskedEmail);
                    return new ResourceNotFoundException("Doctor profile not found for current user");
                });

        if (!currentDoctor.getId().equals(doctorId)) {
            log.warn("Doctor update blocked: user={} tried to update unauthorized doctorId={}", maskedEmail, doctorId);
            throw new UnAuthException("You are not authorized to update this doctor's profile.");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> {
                    log.warn("Doctor update failed: doctorId={} not found in DB", doctorId);
                    return new ResourceNotFoundException("The entered ID is not valid in the Database");
                });

        doctorMapper.update(updateDoctorRequest, doctor);

        User user = doctor.getUser();
        if (user != null) {
            if (updateDoctorRequest.getName() != null) {
                user.setName(updateDoctorRequest.getName());
            }
            if (updateDoctorRequest.getEmail() != null) {
                user.setEmail(updateDoctorRequest.getEmail());
            }
            userRepository.save(user);
        }

        Doctor updatedDoctor = doctorRepository.save(doctor);
        log.info("Doctor updated successfully: doctorId={} by user={}", doctorId, maskedEmail);
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
        User currentUser = securityUtil.getCurrentUser();
        String maskedEmail = LoggerUtil.mask(currentUser.getEmail());

        log.info("Doctor delete attempt by user={} for doctorId={}", maskedEmail, doctorId);

        Doctor currentDoctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> {
                    log.warn("Delete failed: Doctor profile not found for user={}", maskedEmail);
                    return new ResourceNotFoundException("Doctor profile not found for current user");
                });

        if (!currentDoctor.getId().equals(doctorId)) {
            log.warn("Unauthorized delete attempt: user={} tried to delete doctorId={}", maskedEmail, doctorId);
            throw new UnAuthException("You are not authorized to delete this doctor profile.");
        }

        doctorRepository.delete(currentDoctor);
        userRepository.delete(currentUser);

        log.info("Doctor and user deleted successfully: doctorId={}, user={}", doctorId, maskedEmail);
    }

    @Override
    public Page<DoctorAppointmentDto> listAppointments(int page, int size) {
        log.info("Doctor appointment list request: page={}, size={}", page, size);

        if (page < 0 || size <= 0) {
            log.warn("Invalid appointment pagination params: page={}, size={}", page, size);
            throw new InvalidRequestException("Page index must not be negative and size must be greater than zero.");
        }

        User currentUser = securityUtil.getCurrentUser();
        String maskedEmail = LoggerUtil.mask(currentUser.getEmail());

        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> {
                    log.error("Doctor not found for user={}", maskedEmail);
                    return new ResourceNotFoundException("Doctor not found for the logged-in user");
                });

        Pageable pageable = PageRequest.of(page, size);
        Page<DoctorAppointment> appointmentPage = doctorAppointmentRepository.findByDoctorId(doctor.getId(), pageable);

        log.info("Fetched {} appointments for doctorId={}, user={}",
                appointmentPage.getTotalElements(), doctor.getId(), maskedEmail);

        return appointmentPage.map(doctorMapper::toDto);
    }

    @Override
    public DoctorAppointmentDto rescheduleAppointment(Long appointmentId, LocalDateTime newTime) {
        User currentUser = securityUtil.getCurrentUser();
        String maskedEmail = LoggerUtil.mask(currentUser.getEmail());

        log.info("Reschedule attempt: appointmentId={}, newTime={}, user={}", appointmentId, newTime, maskedEmail);

        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> {
                    log.error("Doctor not found for user={}", maskedEmail);
                    return new ResourceNotFoundException("Doctor not found for logged-in user");
                });

        DoctorAppointment doctorAppointment = doctorAppointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    log.warn("Appointment not found: appointmentId={}, user={}", appointmentId, maskedEmail);
                    return new ResourceNotFoundException("DoctorAppointment not found");
                });

        if (!doctorAppointment.getDoctor().getId().equals(doctor.getId())) {
            log.warn("Unauthorized reschedule attempt: appointmentId={}, user={}", appointmentId, maskedEmail);
            throw new RuntimeException("Unauthorized: You can only reschedule your own appointments");
        }

        doctorAppointment.setAppointmentTime(newTime);
        doctorAppointment.setStatus(AppointmentStatus.RESCHEDULED);

        DoctorAppointment saved = doctorAppointmentRepository.save(doctorAppointment);

        log.info("Rescheduled appointmentId={} to {}, doctorId={}, user={}",
                appointmentId, newTime, doctor.getId(), maskedEmail);

        return doctorMapper.toDto(saved);
    }
    @Override
    public void cancelAppointment(Long appointmentId) {
        User currentUser = securityUtil.getCurrentUser();
        String maskedEmail = LoggerUtil.mask(currentUser.getEmail());

        log.info("Cancel appointment request: appointmentId={}, user={}", appointmentId, maskedEmail);

        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> {
                    log.error("Doctor not found for user={}", maskedEmail);
                    return new ResourceNotFoundException("Doctor not found for logged-in user");
                });

        DoctorAppointment doctorAppointment = doctorAppointmentRepository.findById(appointmentId)
                .orElseThrow(() -> {
                    log.warn("Appointment not found: appointmentId={}, user={}", appointmentId, maskedEmail);
                    return new ResourceNotFoundException("DoctorAppointment not found");
                });

        if (!doctorAppointment.getDoctor().getId().equals(doctor.getId())) {
            log.warn("Unauthorized cancel attempt: appointmentId={}, doctorId={}, user={}",
                    appointmentId, doctor.getId(), maskedEmail);
            throw new RuntimeException("Unauthorized: You can only cancel your own appointments");
        }

        doctorAppointment.setStatus(AppointmentStatus.CANCELLED);
        doctorAppointmentRepository.save(doctorAppointment);

        log.info("Successfully cancelled appointmentId={} by doctorId={}, user={}",
                appointmentId, doctor.getId(), maskedEmail);
    }

}

