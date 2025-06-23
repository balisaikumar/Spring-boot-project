package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.PatientDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.Patient;
import com.brinta.hcms.entity.User;
import com.brinta.hcms.enums.Roles;
import com.brinta.hcms.exception.exceptionHandler.EmailAlreadyExistsException;
import com.brinta.hcms.exception.exceptionHandler.InvalidRequestException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.exception.exceptionHandler.UnAuthException;
import com.brinta.hcms.mapper.PatientMapper;
import com.brinta.hcms.repository.PatientRepository;
import com.brinta.hcms.repository.UserRepository;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterPatientRequest;
import com.brinta.hcms.request.updateRequest.UpdatePatientRequest;
import com.brinta.hcms.service.JwtService;
import com.brinta.hcms.service.PatientService;
import com.brinta.hcms.utility.SecurityUtil;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
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

    private static Authentication getAuthentication1(User userEntity) {
        if (!Roles.PATIENT.name().equalsIgnoreCase(userEntity.getRole().name())) {
            throw new RuntimeException("Access denied: Not a patient account");
        }

        // Step 5: Build UserDetails with authorities
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_PATIENT"));
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        userEntity.getEmail(),
                        userEntity.getPassword(),
                        authorities
                );

        // Step 6: Create Authentication with UserDetails as principal
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, authorities
        );
        return authentication;
    }

    private static Authentication getAuthentication(User user) {
        if (!Roles.PATIENT.name().equalsIgnoreCase(user.getRole().name())) {
            throw new RuntimeException("Access denied: Not a patient account");
        }

        // Create an authentication object (with authorities, if needed for JWT)
        List<GrantedAuthority> authorities = List.of(new
                SimpleGrantedAuthority("ROLE_PATIENT"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, authorities
        );
        return authentication;
    }

    @Override
    public Patient registerPatient(RegisterPatientRequest request) {

        boolean emailExists = patientRepository.existsByEmail(request.getEmail());
        boolean contactExists = patientRepository.existsByPatientContactNumber(request.getContactNumber());

        if (emailExists || contactExists) {
            StringBuilder errorMessage = new StringBuilder("Already Exists: ");
            if (emailExists) errorMessage.append("Email: ").append(request.getEmail());
            if (emailExists && contactExists) errorMessage.append(" | ");
            if (contactExists) errorMessage.append("Contact: ").append(request.getContactNumber());
            throw new EmailAlreadyExistsException(errorMessage.toString());
        }

        User user = new User();
        user.setUsername(request.getUserName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Roles.PATIENT);
        user.setName(request.getName());

        Patient patient = patientMapper.register(request, user); // entity mapped with full user
        patient.setUser(user); // just in case
        user.setPatient(patient); // set bidirectional relationship

        userRepository.save(user); // Cascade saves both

        return patient;
    }

    @Override
    public TokenPair patientLogin(LoginRequest request) {

        // Validate input
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password must be provided");
        }

        // Find user by email
        User userEntity = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("User not found with email: " + request.getEmail()));

        // Check password
        if (userEntity.getPassword() == null ||
                !passwordEncoder.matches(request.getPassword(), userEntity.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Ensure a role is PATIENT
        Authentication authentication = getAuthentication1(userEntity);

        // Generate and return TokenPair
        return jwtService.generateTokenPair(authentication);
    }

    @Override
    public Patient update(Long patientId, UpdatePatientRequest updatePatientRequest) {
        // Fetch the patient to update
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("The entered ID is not valid in the Database"));

        // Get currently authenticated user
        User currentUser = securityUtil.getCurrentUser();  // Inject SecurityUtil via constructor

        // Check ownership
        if (!patient.getUser().getId().equals(currentUser.getId())) {
            throw new UnAuthException("You are not authorized to update this patient's details.");
        }

        // Proceed with update
        patientMapper.update(updatePatientRequest, patient);

        User user = patient.getUser();

        if (user != null) {
            if (updatePatientRequest.getName() != null) {
                user.setName(updatePatientRequest.getName());
            }
            if (updatePatientRequest.getEmail() != null) {
                user.setEmail(updatePatientRequest.getEmail());
            }

            userRepository.save(user);
        }

        return patientRepository.save(patient);
    }

    @Override
    public List<PatientDto> findBy(Long patientId, String contactNumber, String email) {
        if (patientId == null && (contactNumber == null || contactNumber.isEmpty())
                && (email == null || email.isEmpty())) {
            throw new ResourceNotFoundException("Enter Correct Input");
        }

        List<Patient> patients = patientRepository.findByParams(patientId, contactNumber, email);

        if (patients.isEmpty()) {
            throw new ResourceNotFoundException("No Matching Patient found in Database");
        }

        return patients.stream().map(patientMapper::findBy).toList();
    }


    public Page<PatientDto> getWithPagination(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new InvalidRequestException("Page index must not be negative and size must be greater than zero.");
        }

        Pageable pageable = PageRequest.of(page, size);


        List<Patient> patientsWithUser = patientRepository.findAllWithUser(pageable);

        // Convert List<Patient> to Page<PatientDto>
        List<PatientDto> dtoList = patientsWithUser.stream()
                .map(patientMapper::toDto)
                .toList();

        return new PageImpl<>(dtoList, pageable, dtoList.size()); // custom page response
    }


}

