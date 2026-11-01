package com.learnpath.version1.controller;

import com.learnpath.version1.dto.DashboardResponse;
import com.learnpath.version1.entities.User;
import com.learnpath.version1.repositories.UserRepository;
import com.learnpath.version1.security.JwtUtil;
import com.learnpath.version1.service.SyllabusService;
import com.learnpath.version1.utility.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserContext userContext;

    @MockitoBean
    private SyllabusService syllabusService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private User currentUser;
    private LocalDateTime createdAt;

    @BeforeEach
    void setUp() {
        createdAt = LocalDateTime.of(2026, 10, 31, 17, 30);
        currentUser = User.builder()
                .id(41L)
                .email("learner@example.com")
                .password("encoded-password")
                .name("Current Learner")
                .username("current_learner")
                .bio("Existing bio")
                .headline("Existing headline")
                .createdAt(createdAt)
                .build();

        when(userContext.getCurrentUser()).thenReturn(currentUser);
        when(userContext.saveUser(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(syllabusService.getDashboardSummary(currentUser))
                .thenReturn(dashboardSummary());
    }

    @Test
    void fetchProfileIncludesStatsAndCreatedAt() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(41))
                .andExpect(jsonPath("$.email").value("learner@example.com"))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-10-31T17:30:00"))
                .andExpect(jsonPath("$.stats.topicsCompleted").value(7))
                .andExpect(jsonPath("$.stats.quizAvgScore").value(82.5));

        verify(userContext).getCurrentUser();
        verify(syllabusService).getDashboardSummary(currentUser);
    }

    @Test
    void updateProfileAcceptsNewUsername() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Updated Learner",
                                  "username": "new_learner",
                                  "bio": "Updated bio",
                                  "headline": "Updated headline"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_learner"));

        verify(userRepository)
                .existsByUsernameAndIdNot("new_learner", currentUser.getId());
        verify(userContext).saveUser(currentUser);
    }

    @Test
    void updateProfileRetainsCurrentUsername() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Updated Learner",
                                  "username": "current_learner",
                                  "bio": "Updated bio",
                                  "headline": "Updated headline"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("current_learner"));

        verify(userRepository).existsByUsernameAndIdNot(
                "current_learner", currentUser.getId());
        verify(userContext).saveUser(currentUser);
    }

    @Test
    void updateProfileRejectsUsernameOwnedByAnotherUserWithoutChangingProfile()
            throws Exception {
        when(userRepository.existsByUsernameAndIdNot(
                "another_user", currentUser.getId())).thenReturn(true);

        mockMvc.perform(put("/api/user/profile")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Changed Name",
                                  "username": "another_user",
                                  "bio": "Changed bio",
                                  "headline": "Changed headline"
                                }
                                """))
                .andExpect(status().isConflict());

        assertEquals("Current Learner", currentUser.getName());
        assertEquals("current_learner", currentUser.getUsername());
        assertEquals("Existing bio", currentUser.getBio());
        assertEquals("Existing headline", currentUser.getHeadline());
        verify(userContext, never()).saveUser(any(User.class));
    }

    @Test
    void updateProfileStoresBlankOptionalUsernameAsNull() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Updated Learner",
                                  "username": "",
                                  "bio": "Updated bio",
                                  "headline": "Updated headline"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(nullValue()));

        assertNull(currentUser.getUsername());
        verify(userContext).saveUser(currentUser);
    }

    @Test
    void updateProfileRejectsUsernameContainingSpaces() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Updated Learner",
                                  "username": "two words",
                                  "bio": null,
                                  "headline": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(userContext, never()).saveUser(any(User.class));
    }

    @Test
    void updateProfileRejectsUsernameContainingPunctuation() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Updated Learner",
                                  "username": "name!",
                                  "bio": null,
                                  "headline": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(userContext, never()).saveUser(any(User.class));
    }

    @Test
    void updateProfileDoesNotChangeEmail() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Updated Learner",
                                  "username": "new_learner",
                                  "bio": "Updated bio",
                                  "headline": "Updated headline"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("learner@example.com"));

        assertEquals("learner@example.com", currentUser.getEmail());
    }

    private DashboardResponse dashboardSummary() {
        return new DashboardResponse(
                1,
                3,
                2,
                9,
                7,
                77.8,
                4,
                3,
                82.5,
                null,
                List.of(),
                List.of());
    }
}
