package com.brinta.hcms.service;

import com.brinta.hcms.dto.DoctorProfileDto;
import com.brinta.hcms.dto.UserDto;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterPatientRequest;

public interface UserService {

    UserDto registerPatient(RegisterPatientRequest request);

    UserDto login(LoginRequest request);

//    DoctorProfileDto registerDoctor(RegisterDoctorRequest request);

    DoctorProfileDto doctorLogin(LoginRequest request);

}
