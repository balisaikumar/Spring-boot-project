package com.brinta.hcms.service.impl;

import com.brinta.hcms.dto.DoctorDto;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.exception.exceptionHandler.InvalidRequestException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.mapper.DoctorMapper;
import com.brinta.hcms.repository.DoctorRepository;
import com.brinta.hcms.request.registerRequest.RegisterDoctor;
import com.brinta.hcms.request.updateRequest.UpdateDoctorRequest;
import com.brinta.hcms.service.DoctorService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Override
    public Doctor register(RegisterDoctor registerDoctor) {

        // Check if email or contact already exists
        boolean emailExists = doctorRepository.existsByEmail(registerDoctor.getEmail());
        boolean contactExists = doctorRepository.existsByContact(registerDoctor.getContact());

        if (emailExists || contactExists) {

            StringBuilder errorMessage = new StringBuilder("Already Exists: ");

            if (emailExists) {
                errorMessage.append("Email: ").append(registerDoctor.getEmail());
            }
            if (emailExists && contactExists) {
                errorMessage.append(" | ");
            }
            if (contactExists) {
                errorMessage.append("Contact: ").append(registerDoctor.getContact());
            }

            throw new DuplicateEntryException(errorMessage.toString());
        }

        Doctor doctor = doctorMapper.register(registerDoctor);

        return doctorRepository.save(doctor);

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
    public List<DoctorDto> findBy(Long doctorId, String contact, String email) {

        //Check Null Input Values
        if (doctorId == null && (contact == null || contact.isEmpty())
                && (email == null || email.isEmpty())) {
            throw new ResourceNotFoundException("Enter Correct Input");
        }

        Optional<Doctor> doctor = doctorRepository.findByIdOrContactOrEmail(doctorId, contact, email);

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
        Page<Doctor> parentPage = doctorRepository.findAll(pageable);

        if (parentPage.isEmpty()) {
            return Page.empty();
        }

        return parentPage.map(doctorMapper::toDto);

    }

    @Override
    public void delete(Long doctorId) {

        //Find Doctor
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(()-> new ResourceNotFoundException("Doctor Not Found"));

        //Delete Doctor
        doctorRepository.delete(doctor);
    }

}
