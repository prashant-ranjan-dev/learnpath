package com.learnpath.version1.controller;

import com.learnpath.version1.entities.User;
import com.learnpath.version1.utility.UserContext;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserContext userContext;


    public UserController(UserContext userContext) {
        this.userContext = userContext;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(){
        User user = userContext.getCurrentUser();
        return ResponseEntity.ok(new UserProfileResponse(user.getName(), user.getEmail()));

    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request){
        User user = userContext.getCurrentUser();
        user.setName(request.name());
        userContext.saveUser(user);
        return ResponseEntity.ok(new UserProfileResponse(user.getName(), user.getEmail()));
    }

    public record UserProfileResponse(String name, String email){}
    public record UpdateProfileRequest(
        @NotBlank (message = "Name is required")
        @Size (max = 100, message = "Name must not exceed 100 characters")
        String name){}
}
