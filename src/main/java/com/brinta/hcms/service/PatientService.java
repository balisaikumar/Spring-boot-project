package com.brinta.hcms.service;

import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.Patient;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterPatientRequest;
import com.brinta.hcms.request.updateRequest.UpdatePatientRequest;

public interface PatientService {

    Patient registerPatient(RegisterPatientRequest request);

    TokenPair patientLogin(LoginRequest request);

    Patient update(Long patientId, UpdatePatientRequest updatePatientRequest);

}
