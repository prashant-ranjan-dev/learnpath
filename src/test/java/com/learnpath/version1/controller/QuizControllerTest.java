
package com.learnpath.version1.controller;

import com.learnpath.version1.dto.QuizAttemptHistoryResponse;
import com.learnpath.version1.security.JwtUtil;
import com.learnpath.version1.service.QuizService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.learnpath.version1.dto.QuizAttemptStartResponse;
import com.learnpath.version1.dto.QuestionDto;
import com.learnpath.version1.dto.QuizResponse;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.time.LocalDateTime;
import java.util.List;
import com.learnpath.version1.dto.QuizResultResponse;
import com.learnpath.version1.dto.QuizSubmissionRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.eq;
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

    @Test
    void getQuizReturnsQuizDetailsAndQuestionsAsJson() throws Exception {
        QuizResponse response = new QuizResponse(
                40L,
                "Java Collections",
                "Test your knowledge of Java Collections",
                70,
                List.of(
                        new QuestionDto(
                                101L,
                                "Which interface represents a collection of unique elements?",
                                "List",
                                "Set",
                                "Queue",
                                "Map"),
                        new QuestionDto(
                                102L,
                                "Which collection follows FIFO ordering?",
                                "Stack",
                                "HashSet",
                                "Queue",
                                "TreeMap")));

        when(quizService.getQuiz(40L)).thenReturn(response);

        mockMvc.perform(get("/api/quizzes/40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizId").value(40))
                .andExpect(jsonPath("$.title").value("Java Collections"))
                .andExpect(jsonPath("$.description")
                        .value("Test your knowledge of Java Collections"))
                .andExpect(jsonPath("$.passMarkPercent").value(70))
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.questions[0].id").value(101))
                .andExpect(jsonPath("$.questions[0].questionText")
                        .value("Which interface represents a collection of unique elements?"))
                .andExpect(jsonPath("$.questions[0].optionA").value("List"))
                .andExpect(jsonPath("$.questions[0].optionB").value("Set"))
                .andExpect(jsonPath("$.questions[0].optionC").value("Queue"))
                .andExpect(jsonPath("$.questions[0].optionD").value("Map"))
                .andExpect(jsonPath("$.questions[1].id").value(102));

        verify(quizService).getQuiz(40L);
    }

    @Test
    void submitQuizReturnsResultAndQuestionReviewAsJson() throws Exception {
        QuizResultResponse response = new QuizResultResponse(
                1,
                2,
                false,
                List.of(1, 0),
                List.of(
                        new QuizResultResponse.QuestionReview(
                                101L,
                                "Which interface represents unique elements?",
                                List.of("List", "Set", "Queue", "Map"),
                                0,
                                1,
                                false,
                                "Set stores unique elements."),
                        new QuizResultResponse.QuestionReview(
                                102L,
                                "Which collection follows FIFO ordering?",
                                List.of("Stack", "HashSet", "Queue", "TreeMap"),
                                2,
                                2,
                                true,
                                "Queue follows FIFO ordering.")),
                70,
                45L);

        when(quizService.submitQuiz(
                eq(40L),
                any(QuizSubmissionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/quizzes/40/submit")
                .contentType("application/json")
                .content("""
                        {
                          "answers": [
                            {"questionId": 101, "selectedIndex": 0},
                            {"questionId": 102, "selectedIndex": 2}
                          ]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(2))
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.correctAnswers[0]").value(1))
                .andExpect(jsonPath("$.correctAnswers[1]").value(0))
                .andExpect(jsonPath("$.passMarkPercent").value(70))
                .andExpect(jsonPath("$.timeTakenSeconds").value(45))
                .andExpect(jsonPath("$.review.length()").value(2))
                .andExpect(jsonPath("$.review[0].questionId").value(101))
                .andExpect(jsonPath("$.review[0].selectedAnswerIndex").value(0))
                .andExpect(jsonPath("$.review[0].correctAnswerIndex").value(1))
                .andExpect(jsonPath("$.review[0].correct").value(false))
                .andExpect(jsonPath("$.review[0].explanation")
                        .value("Set stores unique elements."))
                .andExpect(jsonPath("$.review[1].correct").value(true));

        verify(quizService).submitQuiz(
                eq(40L),
                any(QuizSubmissionRequest.class));
    }

}
