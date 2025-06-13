package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.DoctorProfileDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.dto.UserDto;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.entity.Patient;
import com.brinta.hcms.entity.User;
import com.brinta.hcms.enums.Roles;
import com.brinta.hcms.exception.exceptionHandler.EmailAlreadyExistsException;
import com.brinta.hcms.mapper.DoctorMapper;
import com.brinta.hcms.mapper.PatientMapper;
import com.brinta.hcms.mapper.UserMapper;
import com.brinta.hcms.repository.DoctorRepository;
import com.brinta.hcms.repository.UserRepository;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterPatientRequest;
import com.brinta.hcms.service.JwtService;
import com.brinta.hcms.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientMapper patientMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Override
    public UserDto registerPatient(RegisterPatientRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        Patient profile = patientMapper.toEntity(request);
        profile.getUser().setPassword(passwordEncoder.encode(request.getPassword()));
        profile.getUser().setPatient(profile);

        User savedUser = userRepository.save(profile.getUser());
        return userMapper.toDto(savedUser);
    }

    @Override
    public TokenPair patientLogin(LoginRequest request) {

        // Validate input
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password must be provided");
        }

        // Find user by email
        User userEntity = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + request.getEmail()));

        // Check password
        if (userEntity.getPassword() == null || !passwordEncoder.matches(request.getPassword(), userEntity.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Ensure a role is PATIENT
        Authentication authentication = getAuthentication1(userEntity);

        // Generate and return TokenPair
        return jwtService.generateTokenPair(authentication);
    }

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
    public Map<String, Object> doctorLogin(LoginRequest request) {
        // Validate input
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password must be provided");
        }

        // Fetch the doctor by email
        Doctor doctor = doctorRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Doctor not found with email: "
                                + request.getEmail()));

        // Null check on a user object
        if (doctor.getUser() == null || doctor.getUser().getPassword() == null) {
            throw new RuntimeException("User credentials are not set properly for doctor: "
                    + request.getEmail());
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(),
                doctor.getUser().getPassword())) {
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

        // Convert to DTO
        DoctorProfileDto doctorDto = doctorMapper.toDto(doctor);

        // Return doctor info along with tokens
        return Map.of(
                "message", "Login successful",
                "doctor", doctorDto,
                "accessToken", tokenPair.getAccessToken(),
                "refreshToken", tokenPair.getRefreshToken()
        );
    }

}
