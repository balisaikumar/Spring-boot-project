package com.brinta.hcms.service;


import com.brinta.hcms.dto.RefreshTokenRequest;
import com.brinta.hcms.dto.RegisterRequest;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.User;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.repository.UserRepository;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.utility.LoggerUtil;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Transactional
public class AuthService {

    private UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public void registerUser(RegisterRequest registerRequest) {
        LoggerUtil.info(this.getClass(), "Attempting to register user with username: {}",
                registerRequest.getUsername());

        // Check if user with the same username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            LoggerUtil.warn(this.getClass(), "User already exists with username: {}",
                    registerRequest.getUsername());
            throw new DuplicateEntryException("User is already in use");
        }

        // Create new user and explicitly set isActive to true
        User user = User.builder()
                .name(registerRequest.getName())
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(registerRequest.getRole())
                .build();

        userRepository.save(user);
        LoggerUtil.info(this.getClass(), "User successfully registered with username: {}",
                registerRequest.getUsername());
    }

    public TokenPair login(LoginRequest loginRequest) {
        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        // Set authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate Token Pair
        return jwtService.generateTokenPair(authentication);
    }

    public TokenPair refreshToken(@Valid RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();
        // check if it is a valid refresh token
        if(!jwtService.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String user = jwtService.extractUsernameFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(user);

        if (userDetails == null) {
            throw new IllegalArgumentException("User not found");
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        String accessToken = jwtService.generateAccessToken(authentication);
        return new TokenPair(accessToken, refreshToken);
    }

}

