package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.DoctorDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.entity.User;
import com.brinta.hcms.enums.Roles;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.exception.exceptionHandler.InvalidRequestException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.exception.exceptionHandler.UnAuthException;
import com.brinta.hcms.mapper.DoctorMapper;
import com.brinta.hcms.repository.DoctorRepository;
import com.brinta.hcms.repository.UserRepository;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterDoctorRequest;
import com.brinta.hcms.request.updateRequest.UpdateDoctorRequest;
import com.brinta.hcms.service.DoctorService;
import com.brinta.hcms.service.JwtService;
import com.brinta.hcms.utility.SecurityUtil;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private final DoctorMapper doctorMapper;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

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

        boolean emailExists = doctorRepository.existsByEmail(registerDoctor.getEmail());
        boolean contactExists = doctorRepository.existsByContactNumber(registerDoctor.getContactNumber());

        if (emailExists || contactExists) {
            StringBuilder errorMessage = new StringBuilder("Already Exists: ");
            if (emailExists) errorMessage.append("Email: ").append(registerDoctor.getEmail());
            if (emailExists && contactExists) errorMessage.append(" | ");
            if (contactExists) errorMessage.append("Contact: ").append(registerDoctor.getContactNumber());
            throw new DuplicateEntryException(errorMessage.toString());
        }

        User saveUser = new User();
        saveUser.setUsername(registerDoctor.getUserName());
        saveUser.setName(registerDoctor.getName());
        saveUser.setEmail(registerDoctor.getEmail());
        saveUser.setPassword(passwordEncoder.encode(registerDoctor.getPassword()));
        saveUser.setRole(Roles.DOCTOR);

        Doctor doctor = doctorMapper.register(registerDoctor);
        doctor.setUser(saveUser);
        saveUser.setDoctor(doctor);

        userRepository.save(saveUser);

        return doctor;
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
        DoctorDto doctorDto = doctorMapper.toDto(doctor);

        // Return doctor info along with tokens
        return Map.of(
                "accessToken", tokenPair.getAccessToken(),
                "refreshToken", tokenPair.getRefreshToken()
        );
    }

    @Override
    public Doctor update(Long doctorId, UpdateDoctorRequest updateDoctorRequest) {
        // Get the currently authenticated user
        User currentUser = securityUtil.getCurrentUser();

        // Ensure the user is linked to the doctor they are trying to update
        Doctor currentDoctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new
                        ResourceNotFoundException("Doctor profile not found for current user"));

        // Check if currentDoctor.getId() matches the path variable doctorId
        if (!currentDoctor.getId().equals(doctorId)) {
            throw new UnAuthException("You are not authorized to update this doctor's profile.");
        }

        // Proceed with update
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("The entered ID is not valid in the Database"));

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

        return doctorRepository.save(doctor);
    }

    @Override
    public List<DoctorDto> findBy(Long doctorId, String contactNumber, String email) {

        //Check Null Input Values
        if (doctorId == null && (contactNumber == null || contactNumber.isEmpty())
                && (email == null || email.isEmpty())) {
            throw new ResourceNotFoundException("Enter Correct Input");
        }

        Optional<Doctor> doctor =
                doctorRepository.findByIdOrContactNumberOrEmail(doctorId, contactNumber, email);

        if (doctor.isEmpty()) {
            throw new ResourceNotFoundException("No Matching Doctor found in Database");
        }

        return doctor.stream().map(doctorMapper::findBy).collect(Collectors.toList());

    }

    @Override
    public Page<DoctorDto> getWithPagination(int page, int size) {

        if (page < 0 || size <= 0) {
            throw new
                    InvalidRequestException("Page index must not be negative and size " +
                    "must be greater than zero.");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Doctor> doctorPage = doctorRepository.findAll(pageable);

        if (doctorPage.isEmpty()) {
            return Page.empty();
        }

        return doctorPage.map(doctorMapper::toDto);

    }

    @Override
    public void delete(Long doctorId) {

        // Get currently logged-in user
        User currentUser = securityUtil.getCurrentUser();

        // Fetch doctor linked to the current user
        Doctor currentDoctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new
                        ResourceNotFoundException("Doctor profile not found for current user"));

        // Ensure the current user is deleting only their own profile
        if (!currentDoctor.getId().equals(doctorId)) {
            throw new UnAuthException("You are not authorized to delete this doctor profile.");
        }

        // Delete the doctor and associated user
        doctorRepository.delete(currentDoctor); // First delete doctor

        userRepository.delete(currentUser); // Then delete linked user
    }

}
