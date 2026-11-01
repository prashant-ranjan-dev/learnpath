package com.learnpath.version1.controller;

import com.learnpath.version1.entities.User;
import com.learnpath.version1.utility.UserContext;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.learnpath.version1.dto.DashboardResponse;
import com.learnpath.version1.service.SyllabusService;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserContext userContext;
    private final SyllabusService syllabusService;

    public UserController(
            UserContext userContext,
            SyllabusService syllabusService) {
        this.userContext = userContext;
        this.syllabusService = syllabusService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile() {
        User user = userContext.getCurrentUser();
        return ResponseEntity.ok(toProfileResponse(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {

        User user = userContext.getCurrentUser();
        user.setName(request.name().trim());
        user.setUsername(normalizeOptional(request.username()));
        user.setBio(normalizeOptional(request.bio()));
        user.setHeadline(normalizeOptional(request.headline()));

        userContext.saveUser(user);
        return ResponseEntity.ok(toProfileResponse(user));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        User user = userContext.getCurrentUser();
        return ResponseEntity.ok(
                syllabusService.getDashboardSummary(user));
    }
    
    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getHeadline(),
                user.getCreatedAt());
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record UserProfileResponse(
            Long id,
            String name,
            String email,
            String username,
            String bio,
            String headline,
            java.time.LocalDateTime createdAt) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "Name is required") @Size(max = 100, message = "Name must not exceed 100 characters") String name,

            @Size(max = 50, message = "Username must not exceed 50 characters") @jakarta.validation.constraints.Pattern(regexp = "^[A-Za-z0-9_]*$", message = "Username may contain only letters, numbers, and underscores") String username,

            @Size(max = 500, message = "Bio must not exceed 500 characters") String bio,

            @Size(max = 100, message = "Headline must not exceed 100 characters") String headline) {
    }


}
