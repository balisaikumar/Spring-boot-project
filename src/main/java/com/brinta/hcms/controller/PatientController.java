package com.brinta.hcms.controller;

import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.dto.UserDto;
import com.brinta.hcms.exception.exceptionHandler.EmailAlreadyExistsException;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterPatientRequest;
import com.brinta.hcms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/patient")
@RequiredArgsConstructor
public class PatientController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerPatient(@RequestBody RegisterPatientRequest request) {
        try {
            UserDto user = userService.registerPatient(request);
            return ResponseEntity.status(201)
                    .body(Map.of("message", "Patient registration successful",
                            "Patient", user));
        } catch (EmailAlreadyExistsException ex) {
            return ResponseEntity.status(409).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Something went wrong");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPair> loginPatient(@Valid @RequestBody LoginRequest request) {
        TokenPair tokenPair = userService.patientLogin(request);
        return ResponseEntity.ok(tokenPair);
    }

}
