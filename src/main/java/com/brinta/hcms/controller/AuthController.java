package com.brinta.hcms.controller;

import com.brinta.hcms.dto.RefreshTokenRequest;
import com.brinta.hcms.dto.RegisterRequest;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private AuthService authService;

    // Open endpoint to register a SUPER_ADMIN (First time setup)
    @PostMapping("/register-superadmin")
    public ResponseEntity<String> registerSuperAdmin(@Valid @RequestBody RegisterRequest request) {
        if (!request.getRole().name().equals("SUPER_ADMIN")) {
            return ResponseEntity.badRequest().body("Only SUPER_ADMIN registration allowed here");
        }
        authService.registerUser(request);
        return ResponseEntity.ok("SuperAdmin registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPair> login(@Valid @RequestBody LoginRequest request) {
        TokenPair tokenPair = authService.login(request);
        return ResponseEntity.ok(tokenPair);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenPair> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

}
