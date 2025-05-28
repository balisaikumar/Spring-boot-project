package com.brinta.hcms.controller;

import com.brinta.hcms.dto.UserDto;
import com.brinta.hcms.exception.exceptionHandler.EmailAlreadyExistsException;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterPatientRequest;
import com.brinta.hcms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PatientController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerPatient(@RequestBody RegisterPatientRequest request) {
        try {
             UserDto user  =   userService.registerPatient(request);
            return ResponseEntity.status(201)
                    .body(Map.of("message", "Patient registration successful", "Patient",user ));
        } catch (EmailAlreadyExistsException ex) {
            return ResponseEntity.status(409).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Something went wrong");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            UserDto user = userService.patientLogin(request);
            return ResponseEntity.status(200)
                    .body(Map.of("message","login successful","Patient",user));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

}

