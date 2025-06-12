package com.brinta.hcms.service;

import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.dto.UserDto;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterPatientRequest;

import java.util.Map;

public interface UserService {

    UserDto registerPatient(RegisterPatientRequest request);

    TokenPair patientLogin(LoginRequest request);

    Map<String, Object> doctorLogin(LoginRequest request);

}
