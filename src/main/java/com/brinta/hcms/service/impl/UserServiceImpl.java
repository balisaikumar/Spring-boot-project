package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.DoctorProfileDto;
import com.brinta.hcms.dto.UserDto;
import com.brinta.hcms.entity.AdminProfile;
import com.brinta.hcms.entity.DoctorProfile;
import com.brinta.hcms.entity.PatientProfile;
import com.brinta.hcms.entity.User;
import com.brinta.hcms.enums.Roles;
import com.brinta.hcms.exception.exceptionHandler.EmailAlreadyExistsException;
import com.brinta.hcms.exception.exceptionHandler.InvalidRequestException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.mapper.DoctorMapper;
import com.brinta.hcms.mapper.PatientMapper;
import com.brinta.hcms.mapper.UserMapper;
import com.brinta.hcms.repository.DoctorRepository;
import com.brinta.hcms.repository.UserRepo;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterPatientRequest;
import com.brinta.hcms.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepo userRepo;

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

    @Override
    public UserDto registerPatient(RegisterPatientRequest request) {
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        PatientProfile profile = patientMapper.toEntity(request);
        profile.getUser().setPassword(profile.getUser().getPassword());
        profile.getUser().setPatientProfile(profile);

        User savedUser = userRepo.save(profile.getUser());
        return userMapper.toDto(savedUser);
    }

    @Override
    public UserDto patientLogin(LoginRequest request) {
        return userRepo.findByEmail(request.getEmail())
                .filter(user -> request.getPassword().equals(user.getPassword()))
                .map(userMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
    }

//    @Override
//    public DoctorProfileDto registerDoctor(RegisterDoctorRequest request) {
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new EmailAlreadyExistsException("Email already exists");
//        }
//
//        DoctorProfile profile = doctorMapper.register(request);
//
//        User user = new User();
//        user.setUsername(request.getUserName());
//        user.setEmail(request.getEmail());
//        user.setRole(Roles.DOCTOR);
//
//        String encodedPassword = passwordEncoder.encode(request.getPassword());
//        user.setPassword(encodedPassword);
//
//        profile.setUser(user);
//        user.setDoctorProfile(profile);
//
//        userRepository.save(user);
//
//        return doctorMapper.toDto(profile);
//    }

    @Override
    public DoctorProfileDto doctorLogin(LoginRequest request) {
        // Validate input
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password must be provided");
        }

        // Fetch the doctor by email
        DoctorProfile doctor = doctorRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Doctor not found with email: " + request.getEmail()));

        // Null check on a user object
        if (doctor.getUser() == null || doctor.getUser().getPassword() == null) {
            throw new RuntimeException("User credentials are not set properly for doctor: "
                    + request.getEmail());
        }

        // Compare plain-text passwords
//        if (!doctor.getUser().getPassword().equals(request.getPassword())) {
//            throw new RuntimeException("Invalid password");
//        }

        // Compare Bcrypt passwords
        if (!passwordEncoder.matches(request.getPassword(), doctor.getUser().getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Return profile details
        return doctorMapper.toDto(doctor);
    }

}
