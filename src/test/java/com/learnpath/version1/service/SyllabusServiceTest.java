
package com.learnpath.version1.service;

import com.learnpath.version1.dto.DashboardResponse;
import com.learnpath.version1.entities.QuizAttempt;
import com.learnpath.version1.entities.Syllabus;
import com.learnpath.version1.entities.SyllabusModule;
import com.learnpath.version1.entities.Topic;
import com.learnpath.version1.entities.User;
import com.learnpath.version1.repositories.QuizAttemptRepository;
import com.learnpath.version1.repositories.QuizRepository;
import com.learnpath.version1.repositories.SyllabusRepository;
import com.learnpath.version1.utility.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyllabusServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private SyllabusRepository syllabusRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private UserContext userContext;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    private SyllabusService syllabusService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);

        syllabusService = new SyllabusService(
                chatClientBuilder,
                syllabusRepository,
                objectMapper,
                userContext,
                quizRepository,
                quizAttemptRepository
        );
    }

    @Test
    void getDashboardSummaryCountsOnlyCompletedQuizAttempts() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("test-password")
                .name("Test User")
                .build();

        Topic completedTopic = Topic.builder()
                .id(101L)
                .name("Completed topic")
                .completed(true)
                .build();

        Topic incompleteTopic = Topic.builder()
                .id(102L)
                .name("Incomplete topic")
                .completed(false)
                .build();

        SyllabusModule module = SyllabusModule.builder()
                .id(10L)
                .title("Java Basics")
                .estimatedHours(5.0)
                .topics(new ArrayList<>(List.of(
                        completedTopic,
                        incompleteTopic
                )))
                .build();

        Syllabus syllabus = Syllabus.builder()
                .id(100L)
                .topic("Java")
                .user(user)
                .modules(new ArrayList<>(List.of(module)))
                .build();

        QuizAttempt passedAttempt = QuizAttempt.builder()
                .id(201L)
                .passed(true)
                .userAnswersJson("[0,1,2]")
                .build();

        QuizAttempt failedAttempt = QuizAttempt.builder()
                .id(202L)
                .passed(false)
                .userAnswersJson("[1,0,2]")
                .build();

        when(syllabusRepository.findByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of(syllabus));

        when(quizRepository.findByModuleId(10L))
                .thenReturn(Optional.empty());

        when(quizAttemptRepository
                .findByUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(1L))
                .thenReturn(List.of(passedAttempt, failedAttempt));

        DashboardResponse response =
                syllabusService.getDashboardSummary(user);

        assertEquals(1, response.totalLearningPaths());
        assertEquals(1, response.totalModules());
        assertEquals(0, response.completedModules());

        assertEquals(2, response.totalTopics());
        assertEquals(1, response.completedTopics());
        assertEquals(50.0, response.progressPercent());

        assertEquals(2, response.totalQuizAttempts());
        assertEquals(1, response.passedQuizAttempts());

        assertEquals(1, response.recentLearningPaths().size());
        assertEquals(
                50.0,
                response.recentLearningPaths().get(0).progressPercent()
        );

        verify(quizAttemptRepository)
                .findByUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(1L);
    }
}
