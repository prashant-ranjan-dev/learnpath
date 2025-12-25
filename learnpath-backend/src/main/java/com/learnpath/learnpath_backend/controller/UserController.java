package com.learnpath.learnpath_backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learnpath.learnpath_backend.Service.UserService;
import com.learnpath.learnpath_backend.dto.request.CreateUserRequest;
import com.learnpath.learnpath_backend.model.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public User createUser(@RequestBody CreateUserRequest request){
        User user = User.builder()
            .email(request.getEmail())
            .passwordhash(request.getPassword())
            .fullName(request.getFullName())
            .build();

        return userService.createUser(user);
    }
}
