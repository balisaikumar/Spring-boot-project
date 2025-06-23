package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.PatientDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.Patient;
import com.brinta.hcms.entity.User;
import com.brinta.hcms.enums.PatientRegistrationStatus;
import com.brinta.hcms.enums.ProfileStatus;
import com.brinta.hcms.enums.Roles;
import com.brinta.hcms.exception.exceptionHandler.EmailAlreadyExistsException;
import com.brinta.hcms.exception.exceptionHandler.InvalidRequestException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.exception.exceptionHandler.UnAuthException;
import com.brinta.hcms.mapper.PatientMapper;
import com.brinta.hcms.repository.PatientRepository;
import com.brinta.hcms.repository.UserRepository;
import com.brinta.hcms.request.ForgotPasswordRequest;
import com.brinta.hcms.request.ResetPasswordRequest;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterPatientRequest;
import com.brinta.hcms.request.updateRequest.UpdatePatientRequest;
import com.brinta.hcms.service.EmailService;
import com.brinta.hcms.service.JwtService;
import com.brinta.hcms.service.PatientService;
import com.brinta.hcms.utility.LoggerUtil;
import com.brinta.hcms.utility.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class PatientServiceImpl implements PatientService {

    private static final Class<?> logger = PatientServiceImpl.class;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientMapper patientMapper;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private EmailService emailService;

    private static Authentication getAuthentication1(User userEntity) {
        if (!Roles.PATIENT.name().equalsIgnoreCase(userEntity.getRole().name())) {
            throw new RuntimeException("Access denied: Not a patient account");
        }

        List<GrantedAuthority> authorities = List
                .of(new SimpleGrantedAuthority("ROLE_PATIENT"));

        org.springframework.security.core.userdetails.User userDetails = new org
                .springframework.security.core.userdetails
                .User(userEntity.getEmail(), userEntity.getPassword(), authorities);

        return new UsernamePasswordAuthenticationToken(userDetails, null,
                authorities);
    }

    @Override
    public Patient registerPatientOnline(RegisterPatientRequest request) {
        LoggerUtil.info(logger,
                "Attempting online registration for patient email:" + " {}",
                request.getEmail());

        validateEmailAndContact(request);

        User user = new User();
        user.setUsername(request.getUserName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Roles.PATIENT);
        user.setName(request.getName());

        Patient patient = patientMapper.register(request, user);
        patient.setUser(user);
        patient.setStatus(PatientRegistrationStatus.ONLINE);
        patient.setProfileStatus(ProfileStatus.COMPLETED);

        user.setPatient(patient);

        userRepository.save(user);
        LoggerUtil.info(logger,
                "Online patient registration successful for email: " + "{}",
                request.getEmail());
        return patient;
    }

    @Override
    public Patient registerPatientOffline(RegisterPatientRequest request) {
        LoggerUtil.info(logger,
                "Admin attempting offline registration for" + "patient email: {}",
                request.getEmail());

        validateEmailAndContact(request);

        User user = new User();
        user.setUsername(request.getUserName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Roles.PATIENT);
        user.setName(request.getName());

        Patient patient = patientMapper.register(request, user);
        patient.setUser(user);
        patient.setStatus(PatientRegistrationStatus.OFFLINE);
        patient.setProfileStatus(ProfileStatus.COMPLETED);

        user.setPatient(patient);

        userRepository.save(user);
        LoggerUtil.info(logger, "Offline patient registration successful " +
                "for email: {}", request.getEmail());
        return patient;
    }

    private void validateEmailAndContact(RegisterPatientRequest request) {
        boolean emailExists = patientRepository.existsByEmail(request.getEmail());
        boolean contactExists = patientRepository
                .existsByPatientContactNumber(request.getContactNumber());

        if (emailExists || contactExists) {
            StringBuilder errorMessage = new StringBuilder("Already Exists: ");
            if (emailExists) errorMessage.append("Email: ").append(request.getEmail());
            if (emailExists && contactExists) errorMessage.append(" | ");
            if (contactExists) errorMessage.append("Contact: ")
                    .append(request.getContactNumber());

            LoggerUtil.warn(logger, "Registration failed due to duplication: " +
                    "{}", errorMessage);
            throw new EmailAlreadyExistsException(errorMessage.toString());
        }
    }

    @Override
    public TokenPair patientLogin(LoginRequest request) {
        LoggerUtil.info(logger, "Patient login attempt for email: {}",
                request.getEmail());

        if (request.getEmail() == null || request.getPassword() == null) {
            LoggerUtil.warn(logger, "Login failed: Missing email or password.");
            throw new InvalidRequestException("Email and password must be provided");
        }

        User userEntity = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
            LoggerUtil.warn(logger, "Login failed: No user found with email: {}",
                    request.getEmail());
            return new RuntimeException("User not found with email: " +
                    request.getEmail());
        });

        if (!passwordEncoder.matches(request.getPassword(), userEntity.getPassword())) {
            LoggerUtil.warn(logger, "Login failed: Invalid password for email:"
                    + " {}", request.getEmail());
            throw new RuntimeException("Invalid password");
        }

        Authentication authentication = getAuthentication1(userEntity);

        LoggerUtil.info(logger, "Patient login successful for email: {}",
                request.getEmail());

        return jwtService.generateTokenPair(authentication);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request,
                               HttpServletRequest httpRequest) {
        LoggerUtil.info(logger, "Forgot password attempt for email: {}",
                request.getEmail());

        // Check if user exists by email
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isEmpty()) {
            LoggerUtil.warn(logger, "Forgot password failed: No user found " +
                    "with email: {}", request.getEmail());
            throw new ResourceNotFoundException("User not found with email: " +
                    request.getEmail());
        }

        User user = optionalUser.get(); //Unwrap the Optional

        // Generate unique token
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);

        // Optional: Set token expiry (if you want expiry feature)
        user.setTokenExpiryDate(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);

        // Dynamically build base URL (http/https + domain + port)
        String appUrl = httpRequest.getScheme() + "://" + // http or https
                httpRequest.getServerName() +     // domain name or localhost
                ":" + httpRequest.getServerPort(); // port

        String resetLink = appUrl + "/patient/reset-password?token=" + token;
        String emailContent = "Dear " + user.getUsername() + ",\n\n" +
                "To reset your password, click the following link:\n" +
                resetLink + "\n\nThis link will expire in 15 minutes.\n\nRegards," +
                "\nHealthCare System Team";

        // Send email
        emailService.sendEmail(user.getEmail(), "Password Reset Request",
                emailContent);

        LoggerUtil.info(logger, "Password reset token generated and email sent" +
                " for user: {}", user.getEmail());
    }


    @Override
    public void resetPassword(ResetPasswordRequest request) {
        LoggerUtil.info(logger, "Password reset token attempt with token: {}",
                request.getToken());

        Optional<User> optionalUser = userRepository.findByResetToken(request.getToken());
        if (optionalUser.isEmpty()) {
            throw new ResourceNotFoundException("Invalid password reset token");
        }

        User user = optionalUser.get(); // unwrap Optional

        // Check if token expired
        if (user.getTokenExpiryDate() != null && user.getTokenExpiryDate()
                .isBefore(LocalDateTime.now())) {
            LoggerUtil.warn(logger, "Password reset failed: Token expired " +
                    "for user: {}", user.getEmail());
            throw new RuntimeException("Password reset token has expired. " +
                    "Please request a new one.");
        }

        // Token valid, proceed to reset
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null); // clear token after use

        // Optional: Clear token expiry date too if you have it
        user.setTokenExpiryDate(null);

        userRepository.save(user);

        LoggerUtil.info(logger, "Password reset successful for user: {}",
                user.getEmail());

    }

    @Override
    public Patient update(Long patientId, UpdatePatientRequest updatePatientRequest) {
        Patient patient = patientRepository.findById(patientId).orElseThrow(() -> {
            LoggerUtil.warn(logger, "Update failed: Patient not found with ID:"
                    + " {}", patientId);
            return new ResourceNotFoundException("The entered ID is not valid "
                    + "in the Database");
        });

        User currentUser = securityUtil.getCurrentUser();

        if (!patient.getUser().getId().equals(currentUser.getId())) {
            LoggerUtil.warn(logger, "Unauthorized update attempt by user ID:"
                    + " {} for patient ID: {}", currentUser.getId(), patientId);
            throw new UnAuthException("You are not authorized to update"
                    + " this patient's details.");
        }

        patientMapper.update(updatePatientRequest, patient);

        User user = patient.getUser();
        if (user != null) {
            if (updatePatientRequest.getName() != null)
                user.setName(updatePatientRequest.getName());
            if (updatePatientRequest.getEmail() != null)
                user.setEmail(updatePatientRequest.getEmail());
            userRepository.save(user);
        }

        LoggerUtil.info(logger, "Patient update successful for patient ID: {}",
                patientId);
        return patientRepository.save(patient);
    }

    @Override
    public List<PatientDto> findBy(Long patientId, String contactNumber, String email) {

        if (patientId == null && (contactNumber == null || contactNumber.isEmpty()) &&
                (email == null || email.isEmpty())) {
            LoggerUtil.warn(logger, "FindBy failed: No valid input provided.");
            throw new ResourceNotFoundException("Enter Correct Input");
        }

        List<Patient> patientList = patientRepository
                .findByIdOrContactNumberOrEmail(patientId, contactNumber, email);

        if (patientList == null || patientList.isEmpty()) {
            LoggerUtil.warn(logger, "FindBy failed: No matching patient found "
                    + "for ID: {}, contact: {}, email: {}", patientId, contactNumber, email);
            throw new ResourceNotFoundException("No Matching Patient found in Database");
        }

        LoggerUtil.info(logger, "FindBy successful for {} patient(s).",
                patientList.size());
        return patientList.stream().map(patientMapper::findBy).collect(Collectors.toList());
    }

    @Override
    public Page<PatientDto> getWithPagination(int page, int size) {
        if (page < 0 || size <= 0) {
            LoggerUtil.warn(logger, "Pagination failed: Invalid page" +
                    " index or size.");
            throw new InvalidRequestException("Page index must not be negative " +
                    "and size must be greater than zero.");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Patient> patientPage = patientRepository.findAll(pageable);

        return patientPage.map(patientMapper::toDto);
    }

    @Override
    public void delete(Long patientId) {
        User currentUser = securityUtil.getCurrentUser();

        Patient patientToDelete = patientRepository.findById(patientId)
                .orElseThrow(() -> {
            LoggerUtil.warn(logger, "Delete failed: Patient not found " +
                    "with ID:" + " {}", patientId);
            return new ResourceNotFoundException("Patient not found");
        });

        if (currentUser.getRole().equals(Roles.PATIENT)) {
            if (!patientToDelete.getUser().getId().equals(currentUser.getId())) {
                LoggerUtil.warn(logger, "Unauthorized delete attempt by user ID:"
                        + " {} for patient ID: {}", currentUser.getId(), patientId);
                throw new UnAuthException("You are not authorized to delete " +
                        "this patient profile.");
            }
        } else if (!currentUser.getRole().equals(Roles.ADMIN)) {
            LoggerUtil.warn(logger, "Unauthorized role attempt for delete" +
                    " by user ID: {}", currentUser.getId());
            throw new UnAuthException("Only patient or admin can delete the profile.");
        }

        patientRepository.delete(patientToDelete);
        userRepository.delete(patientToDelete.getUser());
        LoggerUtil.info(logger, "Patient and user deleted successfully" +
                " for patient ID: {}", patientId);
    }

}

