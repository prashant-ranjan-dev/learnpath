package com.learnpath.learnpath_backend.Service;

import org.springframework.stereotype.Service;

import com.learnpath.learnpath_backend.model.User;
import com.learnpath.learnpath_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    private User createUser(User user){
        return userRepository.save(user);
    }

    public User getUserById(Long userId){
        return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("UserNotFound"));
    }

    
}
