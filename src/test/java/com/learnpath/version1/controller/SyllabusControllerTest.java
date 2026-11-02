package com.learnpath.version1.controller;

import com.learnpath.version1.dto.ModuleDto;
import com.learnpath.version1.dto.SyllabusRequest;
import com.learnpath.version1.dto.SyllabusResponse;
import com.learnpath.version1.dto.TopicDto;
import com.learnpath.version1.entities.User;
import com.learnpath.version1.repositories.SyllabusRepository;
import com.learnpath.version1.security.JwtUtil;
import com.learnpath.version1.service.SyllabusService;
import com.learnpath.version1.utility.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SyllabusController.class)
@AutoConfigureMockMvc(addFilters = false)
class SyllabusControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private SyllabusService syllabusService;

        @MockitoBean
        private SyllabusRepository syllabusRepository;

        @MockitoBean
        private UserContext userContext;

        @MockitoBean
        private JwtUtil jwtUtil;

        @MockitoBean
        private UserDetailsService userDetailsService;

        @Test
        void postSyllabusReturnsGeneratedLearningPath() throws Exception {
                SyllabusRequest request = new SyllabusRequest(
                                "Java", "Beginner", "Intermediate", "Build backend applications");
                SyllabusResponse generatedPath = learningPath(601L, "Java");
                when(syllabusService.generateSyllabus(request)).thenReturn(generatedPath);

                mockMvc.perform(post("/api/syllabus")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "topic": "Java",
                                                  "currentUnderstanding": "Beginner",
                                                  "depthLevel": "Intermediate",
                                                  "goal": "Build backend applications"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.syllabusId").value(601))
                                .andExpect(jsonPath("$.topic").value("Java"))
                                .andExpect(jsonPath("$.description").value("A Java learning path"))
                                .andExpect(jsonPath("$.modules[0].title").value("Java Fundamentals"))
                                .andExpect(jsonPath("$.modules[0].topics[0].name").value("Variables"))
                                .andExpect(jsonPath("$.description")
                                                .value("A Java learning path"))
                                .andExpect(jsonPath("$.modules[0].description")
                                                .value("Learn Java basics"))
                                .andExpect(jsonPath("$.modules[0].topics[0].description")
                                                .value("Learn variables"));

                verify(syllabusService).generateSyllabus(request);
        }

        @Test
        void postSyllabusRejectsBlankRequiredFields() throws Exception {
                mockMvc.perform(post("/api/syllabus")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "topic": " ",
                                                  "currentUnderstanding": "",
                                                  "depthLevel": "Intermediate",
                                                  "goal": "Build backend applications"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                verify(syllabusService, never())
                                .generateSyllabus(any(SyllabusRequest.class));
        }

        @Test
        void getMySyllabiReturnsOnlyPathsForCurrentUser() throws Exception {
                User currentUser = user(11L, "current@example.com");
                when(userContext.getCurrentUser()).thenReturn(currentUser);
                when(syllabusService.getMySyllabi(currentUser))
                                .thenReturn(List.of(learningPath(601L, "Java")));

                mockMvc.perform(get("/api/syllabus/my"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].syllabusId").value(601))
                                .andExpect(jsonPath("$[0].topic").value("Java"))
                                .andExpect(jsonPath("$[0].description")
                                                .value("A Java learning path"))
                                .andExpect(jsonPath("$[0].modules[0].description")
                                                .value("Learn Java basics"))
                                .andExpect(jsonPath("$[0].modules[0].topics[0].description")
                                                .value("Learn variables"));

                verify(userContext).getCurrentUser();
                verify(syllabusService).getMySyllabi(currentUser);
        }

        @Test
        void getSyllabusByIdReturnsRequestedPathForCurrentUser() throws Exception {
                User currentUser = user(11L, "current@example.com");
                SyllabusResponse requestedPath = learningPath(601L, "Java");
                when(userContext.getCurrentUser()).thenReturn(currentUser);
                when(syllabusService.getSyllabusById(601L, currentUser))
                                .thenReturn(requestedPath);

                mockMvc.perform(get("/api/syllabus/601"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.syllabusId").value(601))
                                .andExpect(jsonPath("$.topic").value("Java"))
                                .andExpect(jsonPath("$.modules[0].title").value("Java Fundamentals"))
                                .andExpect(jsonPath("$.description")
                                                .value("A Java learning path"))
                                .andExpect(jsonPath("$.modules[0].description")
                                                .value("Learn Java basics"))
                                .andExpect(jsonPath("$.modules[0].topics[0].description")
                                                .value("Learn variables"));

                verify(userContext).getCurrentUser();
                verify(syllabusService).getSyllabusById(601L, currentUser);
        }

        private static User user(Long id, String email) {
                return User.builder()
                                .id(id)
                                .email(email)
                                .name("Current User")
                                .password("encoded-password")
                                .build();
        }

        private static SyllabusResponse learningPath(Long id, String topic) {
                return new SyllabusResponse(
                                id,
                                topic,
                                "A Java learning path",
                                "Beginner",
                                "Build backend applications",
                                4.5,
                                0.0,
                                null,
                                List.of(new ModuleDto(
                                                701L,
                                                "Java Fundamentals",
                                                "Learn Java basics",
                                                4.5,
                                                false,
                                                true,
                                                0,
                                                List.of(new TopicDto(
                                                                801L, "Variables", "Learn variables", 30, false)))));
        }
}
