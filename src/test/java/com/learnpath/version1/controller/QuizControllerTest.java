
package com.learnpath.version1.controller;

import com.learnpath.version1.dto.QuizAttemptHistoryResponse;
import com.learnpath.version1.security.JwtUtil;
import com.learnpath.version1.service.QuizService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.learnpath.version1.dto.QuizAttemptStartResponse;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(QuizController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuizService quizService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void getQuizAttemptHistoryReturnsCompletedAttemptsAsJson() throws Exception {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 10, 31, 17, 30);
        LocalDateTime startedAt = submittedAt.minusMinutes(2);

        List<QuizAttemptHistoryResponse> history = List.of(
                new QuizAttemptHistoryResponse(
                        502L,
                        40L,
                        4,
                        5,
                        true,
                        submittedAt,
                        startedAt,
                        submittedAt,
                        120L),
                new QuizAttemptHistoryResponse(
                        501L,
                        40L,
                        2,
                        5,
                        false,
                        submittedAt.minusDays(1),
                        startedAt.minusDays(1),
                        submittedAt.minusDays(1),
                        90L));

        when(quizService.getQuizAttemptHistory(40L)).thenReturn(history);

        mockMvc.perform(get("/api/quizzes/40/attempts/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].attemptId").value(502))
                .andExpect(jsonPath("$[0].quizId").value(40))
                .andExpect(jsonPath("$[0].score").value(4))
                .andExpect(jsonPath("$[0].totalQuestions").value(5))
                .andExpect(jsonPath("$[0].passed").value(true))
                .andExpect(jsonPath("$[0].timeTakenSeconds").value(120))
                .andExpect(jsonPath("$[1].attemptId").value(501))
                .andExpect(jsonPath("$[1].score").value(2))
                .andExpect(jsonPath("$[1].passed").value(false));

        verify(quizService).getQuizAttemptHistory(40L);
    }

    @Test
    void getQuizAttemptHistoryReturnsEmptyArrayWhenNoAttemptsExist()
            throws Exception {

        when(quizService.getQuizAttemptHistory(40L)).thenReturn(List.of());

        mockMvc.perform(get("/api/quizzes/40/attempts/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(quizService).getQuizAttemptHistory(40L);
    }

    @Test
    void startAttemptReturnsAttemptDetailsAsJson() throws Exception {
        LocalDateTime startedAt = LocalDateTime.of(2026, 10, 31, 18, 0);

        QuizAttemptStartResponse response = new QuizAttemptStartResponse(
                700L,
                40L,
                startedAt);

        when(quizService.startAttempt(40L)).thenReturn(response);

        mockMvc.perform(post("/api/quizzes/40/attempts/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(700))
                .andExpect(jsonPath("$.quizId").value(40))
                .andExpect(jsonPath("$.startedAt").value("2026-10-31T18:00:00"));

        verify(quizService).startAttempt(40L);
    }
}
