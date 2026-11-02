package com.learnpath.version1.controller;

import com.learnpath.version1.entities.User;
import com.learnpath.version1.repositories.UserRepository;
import com.learnpath.version1.security.JwtUtil;
import com.learnpath.version1.service.AuthService;
import com.learnpath.version1.service.AuthService.EmailAlreadyRegisteredException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void signupReturnsCreatedResponseWithTokenAndUserIdentity() throws Exception {
        User user = User.builder()
                .id(42L)
                .name("Ada Lovelace")
                .email("ada@example.com")
                .password("encoded-password")
                .build();
        when(authService.registerUser(
                "ada@example.com", "password123", "Ada Lovelace"))
                .thenReturn(user);
        when(jwtUtil.generateToken(any()))
                .thenReturn("signed-jwt-token");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ada@example.com",
                                  "password": "password123",
                                  "name": "Ada Lovelace"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.id").value(42))
                .andExpect(jsonPath("$.user.name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.user.email").value("ada@example.com"));

        verify(authService).registerUser(
                "ada@example.com", "password123", "Ada Lovelace");
    }

    @Test
    void signupReturnsConflictWhenEmailIsAlreadyRegistered() throws Exception {
        when(authService.registerUser(
                "ada@example.com", "password123", "Ada Lovelace"))
                .thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ada@example.com",
                                  "password": "password123",
                                  "name": "Ada Lovelace"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already registered"))
                .andExpect(jsonPath("$.message").value("Email already registered"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void signupRejectsInvalidEmail() throws Exception {
        assertSignupRequestIsInvalid("""
                {
                  "email": "not-an-email",
                  "password": "password123",
                  "name": "Ada Lovelace"
                }
                """);
    }

    @Test
    void signupRejectsBlankName() throws Exception {
        assertSignupRequestIsInvalid("""
                {
                  "email": "ada@example.com",
                  "password": "password123",
                  "name": " "
                }
                """);
    }

    @Test
    void signupRejectsPasswordShorterThanEightCharacters() throws Exception {
        assertSignupRequestIsInvalid("""
                {
                  "email": "ada@example.com",
                  "password": "short7",
                  "name": "Ada Lovelace"
                }
                """);
    }

    private void assertSignupRequestIsInvalid(String requestBody)
            throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(authService, never()).registerUser(
                anyString(),
                anyString(),
                anyString());
    }
}
