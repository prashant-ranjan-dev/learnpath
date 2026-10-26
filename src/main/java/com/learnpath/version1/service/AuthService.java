package com.learnpath.version1.service;

import com.learnpath.version1.entities.User;
import com.learnpath.version1.repositories.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(String email, String password, String name) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .build();

        return userRepository.saveAndFlush(user);
    }

    public static class EmailAlreadyRegisteredException
            extends RuntimeException {

        public EmailAlreadyRegisteredException() {
            super("Email already registered");
        }
    }
}
