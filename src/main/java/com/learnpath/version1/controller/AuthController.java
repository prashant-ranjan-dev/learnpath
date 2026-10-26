
package com.learnpath.version1.controller;

import com.learnpath.version1.entities.User;
import com.learnpath.version1.repositories.UserRepository;
import com.learnpath.version1.security.JwtUtil;
import com.learnpath.version1.service.AuthService;
import com.learnpath.version1.service.AuthService.EmailAlreadyRegisteredException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final AuthService authService;

    public AuthController(
            UserRepository userRepository,
            JwtUtil jwtUtil,
            AuthenticationManager authenticationManager,
            AuthService authService) {

        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(
            @Valid @RequestBody SignupRequest request) {

        try {
            User user = authService.registerUser(
                    request.email(),
                    request.password(),
                    request.name()
            );

            UserDetails userDetails =
                    org.springframework.security.core.userdetails.User.builder()
                            .username(user.getEmail())
                            .password(user.getPassword())
                            .roles("USER")
                            .build();

            String token = jwtUtil.generateToken(userDetails);

            return ResponseEntity.ok(Map.of("token", token));

        } catch (EmailAlreadyRegisteredException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.email(),
                                request.password()
                        )
                );

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        String token = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(Map.of("token", token));
    }

    public record SignupRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            @Size(max = 254, message = "Email must not exceed 254 characters")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 72,
                    message = "Password must be between 8 and 72 characters")
            String password,

            @NotBlank(message = "Name is required")
            @Size(max = 100, message = "Name must not exceed 100 characters")
            String name
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            @Size(max = 254, message = "Email must not exceed 254 characters")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {}
}
