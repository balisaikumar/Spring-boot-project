package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.DoctorProfileDto;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.entity.User;
import com.brinta.hcms.enums.Roles;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.exception.exceptionHandler.InvalidRequestException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.mapper.DoctorMapper;
import com.brinta.hcms.repository.DoctorRepository;
import com.brinta.hcms.repository.UserRepository;
import com.brinta.hcms.request.registerRequest.RegisterDoctorRequest;
import com.brinta.hcms.request.updateRequest.UpdateDoctorRequest;
import com.brinta.hcms.service.DoctorService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
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
    public Doctor update(Long doctorId, UpdateDoctorRequest updateDoctorRequest) {

        // Check if doctor exists
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new
                        ResourceNotFoundException("The entered ID is not valid in the Database"));

        doctorMapper.update(updateDoctorRequest, doctor);

        return doctorRepository.save(doctor);

    }

    @Override
    public List<DoctorProfileDto> findBy(Long doctorId, String contactNumber, String email) {

        //Check Null Input Values
        if (doctorId == null && (contactNumber == null || contactNumber.isEmpty())
                && (email == null || email.isEmpty())) {
            throw new ResourceNotFoundException("Enter Correct Input");
        }

        Optional<Doctor> doctor = doctorRepository.findByIdOrContactNumberOrEmail(doctorId, contactNumber, email);

        if (doctor.isEmpty()) {
            throw new ResourceNotFoundException("No Matching Doctor found in Database");
        }

        return doctor.stream().map(doctorMapper::findBy).collect(Collectors.toList());

    }

    @Override
    public Page<DoctorProfileDto> getWithPagination(int page, int size) {

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

        //Find Doctor
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor Not Found"));

        //Delete Doctor
        doctorRepository.delete(doctor);
    }

}
