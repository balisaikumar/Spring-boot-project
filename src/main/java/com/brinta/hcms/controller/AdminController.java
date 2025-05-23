package com.brinta.hcms.controller;

import com.brinta.hcms.dto.DoctorProfileDto;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@AllArgsConstructor
public class AdminController {

    private final UserService userService;

//    @PostMapping("/register-doctor")
//    public ResponseEntity<?> registerDoctor(@Valid @RequestBody RegisterDoctorRequest request) {
//        try {
//            DoctorProfileDto doctor = userService.registerDoctor(request);
//            return ResponseEntity.status(201)
//                    .body(Map.of("message", "Doctor registration successful", "Doctor", doctor));
//        } catch (EmailAlreadyExistsException ex) {
//            return ResponseEntity.status(409).body(ex.getMessage());
//        }
//
//    }

    @PostMapping("/login-doctor")
    public ResponseEntity<?> doctorLogin(@RequestBody LoginRequest request) {
        try {
            DoctorProfileDto doctor = userService.doctorLogin(request);
            return ResponseEntity.status(200)
                    .body(Map.of("message", "Login successful", "Doctor", doctor));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).body(ex.getMessage());
        }
    }

}
