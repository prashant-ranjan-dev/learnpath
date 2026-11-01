
package com.learnpath.version1.controller;

import com.learnpath.version1.dto.DashboardResponse;
import com.learnpath.version1.entities.User;
import com.learnpath.version1.repositories.UserRepository;
import com.learnpath.version1.service.SyllabusService;
import com.learnpath.version1.utility.UserContext;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserContext userContext;
    private final SyllabusService syllabusService;
    private final UserRepository userRepository;

    public UserController(
            UserContext userContext,
            SyllabusService syllabusService,
            UserRepository userRepository) {
        this.userContext = userContext;
        this.syllabusService = syllabusService;
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile() {
        User user = userContext.getCurrentUser();
        return ResponseEntity.ok(toProfileResponse(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {

        User user = userContext.getCurrentUser();

        String username = normalizeOptional(request.username());

        if (username != null
                && userRepository.existsByUsernameAndIdNot(
                        username, user.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "Username already taken",
                            "message", "Please choose a different username."));
        }

        user.setName(request.name().trim());
        user.setUsername(username);
        user.setBio(normalizeOptional(request.bio()));
        user.setHeadline(normalizeOptional(request.headline()));

        User savedUser = userContext.saveUser(user);

        return ResponseEntity.ok(toProfileResponse(savedUser));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        User user = userContext.getCurrentUser();
        return ResponseEntity.ok(
                syllabusService.getDashboardSummary(user));
    }

    private UserProfileResponse toProfileResponse(User user) {
        DashboardResponse dashboard =
                syllabusService.getDashboardSummary(user);

        ProfileStats stats = new ProfileStats(
                dashboard.completedTopics(),
                dashboard.averageQuizScorePercent());

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getHeadline(),
                user.getCreatedAt(),
                stats);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record ProfileStats(
            int topicsCompleted,
            double quizAvgScore) {
    }

    public record UserProfileResponse(
            Long id,
            String name,
            String email,
            String username,
            String bio,
            String headline,
            LocalDateTime createdAt,
            ProfileStats stats) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 100, message = "Name must not exceed 100 characters")
            String name,

            @Size(max = 50, message = "Username must not exceed 50 characters")
            @Pattern(
                    regexp = "^[A-Za-z0-9_]*$",
                    message = "Username may contain only letters, numbers, and underscores")
            String username,

            @Size(max = 500, message = "Bio must not exceed 500 characters")
            String bio,

            @Size(max = 100, message = "Headline must not exceed 100 characters")
            String headline) {
    }
}
