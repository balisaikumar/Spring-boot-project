package com.brinta.hcms.service;

import com.brinta.hcms.dto.DoctorProfileDto;
import com.brinta.hcms.dto.UserDto;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterPatientRequest;

public interface UserService {

    UserDto registerPatient(RegisterPatientRequest request);

    UserDto patientLogin(LoginRequest request);

    DoctorProfileDto doctorLogin(LoginRequest request);

}

