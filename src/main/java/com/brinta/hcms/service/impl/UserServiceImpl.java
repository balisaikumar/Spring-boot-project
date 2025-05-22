package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.DoctorProfileDto;
import com.brinta.hcms.dto.UserDto;
import com.brinta.hcms.entity.DoctorProfile;
import com.brinta.hcms.entity.PatientProfile;
import com.brinta.hcms.entity.User;
import com.brinta.hcms.enums.Roles;
import com.brinta.hcms.exceptions.EmailAlreadyExistsException;
import com.brinta.hcms.mappers.DoctorMapper;
import com.brinta.hcms.mappers.PatientMapper;
import com.brinta.hcms.mappers.UserMapper;
import com.brinta.hcms.repository.UserRepo;
import com.brinta.hcms.request.LoginRequest;
import com.brinta.hcms.request.RegisterDoctorRequest;
import com.brinta.hcms.request.RegisterPatientRequest;
import com.brinta.hcms.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepo userRepository;
    private final PatientMapper patientMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final DoctorMapper doctorMapper;

    @Override
    public UserDto registerPatient(RegisterPatientRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        PatientProfile profile = patientMapper.toEntity(request);
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        profile.getUser().setPassword(encodedPassword);
        profile.getUser().setPatientProfile(profile);

        User savedUser = userRepository.save(profile.getUser());
        return userMapper.toDto(savedUser);
    }

    @Override
    public UserDto login(LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(userMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
    }

    @Override
    public DoctorProfileDto registerDoctor(RegisterDoctorRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        DoctorProfile profile = doctorMapper.toEntity(request);

        User user = new User();
        user.setUsername(request.getUserName());
        user.setEmail(request.getEmail());
        user.setRole(Roles.DOCTOR);

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encodedPassword);

        profile.setUser(user);
        user.setDoctorProfile(profile);

        userRepository.save(user);

        return doctorMapper.toDto(profile);
    }

    @Override
    public DoctorProfileDto doctorLogin(LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(user -> doctorMapper.toDto(user.getDoctorProfile()))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
    }

}

