package com.brinta.hcms.service;

import com.brinta.hcms.dto.DoctorDto;
import com.brinta.hcms.dto.PatientDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.Patient;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterPatientRequest;
import com.brinta.hcms.request.updateRequest.UpdatePatientRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PatientService {

    Patient registerPatient(RegisterPatientRequest request);

    TokenPair patientLogin(LoginRequest request);

    Patient update(Long patientId, UpdatePatientRequest updatePatientRequest);

    List<PatientDto> findBy(Long patientId, String contactNumber, String email);

    Page<PatientDto> getWithPagination(int page, int size);

}
