package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.PatientDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.Branch;
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
import com.brinta.hcms.repository.BranchRepository;
import com.brinta.hcms.repository.PatientRepository;
import com.brinta.hcms.repository.UserRepository;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterPatientRequest;
import com.brinta.hcms.request.updateRequest.UpdatePatientRequest;
import com.brinta.hcms.service.JwtService;
import com.brinta.hcms.service.PatientService;
import com.brinta.hcms.utility.LoggerUtil;
import com.brinta.hcms.utility.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
@Transactional
public class PatientServiceImpl implements PatientService {

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
    private BranchRepository branchRepository;

    private static Authentication getAuthentication1(User userEntity) {
        if (!Roles.PATIENT.name().equalsIgnoreCase(userEntity.getRole().name())) {
            throw new RuntimeException("Access denied: Not a patient account");
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_PATIENT"));

        org.springframework.security.core.userdetails.User userDetails = new org
                .springframework.security.core.userdetails
                .User(userEntity.getEmail(), userEntity.getPassword(), authorities);

        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    @Override
    public Patient registerPatientOnline(RegisterPatientRequest request) {
        log.info("Attempting online registration for patient email: {}",
                LoggerUtil.mask(request.getEmail()));

        validateEmailAndContact(request);

        User user = new User();
        user.setUsername(request.getUserName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Roles.PATIENT);
        user.setName(request.getName());

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        Patient patient = patientMapper.register(request, user, branch);
        patient.setUser(user);
        patient.setStatus(PatientRegistrationStatus.ONLINE);
        patient.setProfileStatus(ProfileStatus.COMPLETED);

        user.setPatient(patient);

        userRepository.save(user);
        log.info("Online patient registration successful for email: {}",
                LoggerUtil.mask(request.getEmail()));
        return patient;
    }

    @Override
    public Patient registerPatientOffline(RegisterPatientRequest request) {
        log.info("Admin attempting offline registration for patient email: {}",
                LoggerUtil.mask(request.getEmail()));

        validateEmailAndContact(request);

        User user = new User();
        user.setUsername(request.getUserName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Roles.PATIENT);
        user.setName(request.getName());

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        Patient patient = patientMapper.register(request, user, branch);
        patient.setUser(user);
        patient.setStatus(PatientRegistrationStatus.OFFLINE);
        patient.setProfileStatus(ProfileStatus.COMPLETED);

        user.setPatient(patient);

        userRepository.save(user);
        log.info("Offline patient registration successful for email: {}",
                LoggerUtil.mask(request.getEmail()));
        return patient;
    }

    private void validateEmailAndContact(RegisterPatientRequest request) {
        boolean emailExists = patientRepository.existsByEmail(request.getEmail());
        boolean contactExists = patientRepository
                .existsByPatientContactNumber(request.getContactNumber());

        if (emailExists || contactExists) {
            StringBuilder errorMessage = new StringBuilder("Already Exists: ");
            if (emailExists) errorMessage.append("Email: ")
                    .append(LoggerUtil.mask(request.getEmail()));
            if (emailExists && contactExists) errorMessage.append(" | ");
            if (contactExists) errorMessage.append("Contact: ")
                    .append(LoggerUtil.mask(request.getContactNumber()));

            log.warn("Registration failed due to duplication: {}", errorMessage);
            throw new EmailAlreadyExistsException(errorMessage.toString());
        }
    }

    @Override
    public TokenPair patientLogin(LoginRequest request) {
        log.info("Patient login attempt for email: {}", LoggerUtil.mask(request.getEmail()));

        if (request.getEmail() == null || request.getPassword() == null) {
            log.warn("Login failed: Missing email or password.");
            throw new InvalidRequestException("Email and password must be provided");
        }

        User userEntity = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: No user found with email: {}",
                            LoggerUtil.mask(request.getEmail()));
                    return new RuntimeException("User not found with email: " + request.getEmail());
                });

        if (!passwordEncoder.matches(request.getPassword(), userEntity.getPassword())) {
            log.warn("Login failed: Invalid password for email: {}", LoggerUtil.mask(request.getEmail()));
            throw new RuntimeException("Invalid password");
        }

        Authentication authentication = getAuthentication1(userEntity);

        log.info("Patient login successful for email: {}", LoggerUtil.mask(request.getEmail()));

        return jwtService.generateTokenPair(authentication);
    }

    @Override
    public Patient update(Long patientId, UpdatePatientRequest updatePatientRequest) {
        Patient patient = patientRepository.findById(patientId).orElseThrow(() -> {
            log.warn("Update failed: Patient not found with ID: {}", patientId);
            return new ResourceNotFoundException("The entered ID is not valid in the Database");
        });

        User currentUser = securityUtil.getCurrentUser();

        if (!patient.getUser().getId().equals(currentUser.getId())) {
            log.warn("Unauthorized update attempt by user ID: {} for patient ID: {}",
                    currentUser.getId(), patientId);
            throw new UnAuthException("You are not authorized to update this patient's details.");
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

        log.info("Patient update successful for patient ID: {}", patientId);
        return patientRepository.save(patient);
    }

    @Override
    public List<PatientDto> findBy(Long patientId, String contactNumber, String email) {

        if (patientId == null && (contactNumber == null || contactNumber.isEmpty()) &&
                (email == null || email.isEmpty())) {
            log.warn("FindBy failed: No valid input provided.");
            throw new ResourceNotFoundException("Enter Correct Input");
        }

        List<Patient> patientList = patientRepository.findByParams(patientId, contactNumber, email);

        if (patientList == null || patientList.isEmpty()) {
            log.warn("FindBy failed: No matching patient found for ID: {}, contact: {}, email: {}",
                    patientId, LoggerUtil.mask(contactNumber), LoggerUtil.mask(email));
            throw new ResourceNotFoundException("No Matching Patient found in Database");
        }

        log.info("FindBy successful for {} patient(s).", patientList.size());
        return patientList.stream().map(patientMapper::findBy).collect(Collectors.toList());
    }

    @Override
    public Page<PatientDto> getWithPagination(int page, int size) {
        if (page < 0 || size <= 0) {
            log.warn("Pagination failed: Invalid page index or size.");
            throw new InvalidRequestException("Page index must not be negative and " +
                    "size must be greater than zero.");
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
                    log.warn("Delete failed: Patient not found with ID: {}", patientId);
                    return new ResourceNotFoundException("Patient not found");
                });

        if (currentUser.getRole().equals(Roles.PATIENT)) {
            if (!patientToDelete.getUser().getId().equals(currentUser.getId())) {
                log.warn("Unauthorized delete attempt by user ID: {} for patient ID: {}",
                        currentUser.getId(), patientId);
                throw new
                        UnAuthException("You are not authorized to delete this patient profile.");
            }
        } else if (!currentUser.getRole().equals(Roles.ADMIN)) {
            log.warn("Unauthorized role attempt for delete by user ID: {}", currentUser.getId());
            throw new UnAuthException("Only patient or admin can delete the profile.");
        }

        patientRepository.delete(patientToDelete);
        userRepository.delete(patientToDelete.getUser());
        log.info("Patient and user deleted successfully for patient ID: {}", patientId);
    }

}

