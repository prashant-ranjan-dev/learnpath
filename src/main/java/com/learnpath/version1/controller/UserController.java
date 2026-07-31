package com.learnpath.version1.controller;

import com.learnpath.version1.entities.User;
import com.learnpath.version1.utility.UserContext;
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
    public ResponseEntity<UserProfileResponse> updateProfile(@RequestBody UpdateProfileRequest request){
        User user = userContext.getCurrentUser();
        if (request.name() != null && !request.name().isBlank()){
            user.setName(request.name());
            userContext.saveUser(user);
        }
        return ResponseEntity.ok(new UserProfileResponse(user.getName(), user.getEmail()));
    }

    public record UserProfileResponse(String name, String email){}
    public record UpdateProfileRequest(String name){}
}
