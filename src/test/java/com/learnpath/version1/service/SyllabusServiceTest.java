
package com.learnpath.version1.service;

import com.learnpath.version1.dto.DashboardResponse;
import com.learnpath.version1.entities.Quiz;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
                                quizAttemptRepository);
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
                                                incompleteTopic)))
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

                DashboardResponse response = syllabusService.getDashboardSummary(user);

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
                                response.recentLearningPaths().get(0).progressPercent());

                verify(quizAttemptRepository)
                                .findByUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(1L);
        }

        @Test
        void getDashboardSummaryReturnsZerosWhenUserHasNoLearningPathsOrAttempts() {
                User user = User.builder()
                                .id(2L)
                                .email("empty@example.com")
                                .password("test-password")
                                .name("Empty User")
                                .build();

                when(syllabusRepository.findByUserOrderByCreatedAtDesc(user))
                                .thenReturn(List.of());

                when(quizAttemptRepository
                                .findByUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(2L))
                                .thenReturn(List.of());

                DashboardResponse response = syllabusService.getDashboardSummary(user);

                assertEquals(0, response.totalLearningPaths());
                assertEquals(0, response.totalModules());
                assertEquals(0, response.completedModules());
                assertEquals(0, response.totalTopics());
                assertEquals(0, response.completedTopics());
                assertEquals(0.0, response.progressPercent());
                assertEquals(0, response.totalQuizAttempts());
                assertEquals(0, response.passedQuizAttempts());
                assertEquals(0, response.recentLearningPaths().size());
                assertEquals(null, response.continueLearning());
                assertEquals(0, response.recentActivity().size());
        }

        @Test
        void getDashboardSummaryReturnsOneHundredPercentWhenAllTopicsAreCompleted() {
                User user = User.builder()
                                .id(3L)
                                .email("completed@example.com")
                                .password("test-password")
                                .name("Completed User")
                                .build();

                Topic topicOne = Topic.builder()
                                .id(301L)
                                .name("Java Fundamentals")
                                .completed(true)
                                .build();

                Topic topicTwo = Topic.builder()
                                .id(302L)
                                .name("Java Collections")
                                .completed(true)
                                .build();

                SyllabusModule module = SyllabusModule.builder()
                                .id(30L)
                                .title("Java Basics")
                                .estimatedHours(5.0)
                                .topics(new ArrayList<>(List.of(topicOne, topicTwo)))
                                .build();

                Syllabus syllabus = Syllabus.builder()
                                .id(300L)
                                .topic("Java")
                                .user(user)
                                .modules(new ArrayList<>(List.of(module)))
                                .build();

                when(syllabusRepository.findByUserOrderByCreatedAtDesc(user))
                                .thenReturn(List.of(syllabus));

                when(quizRepository.findByModuleId(30L))
                                .thenReturn(Optional.empty());

                when(quizAttemptRepository
                                .findByUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(3L))
                                .thenReturn(List.of());

                DashboardResponse response = syllabusService.getDashboardSummary(user);

                assertEquals(1, response.totalLearningPaths());
                assertEquals(2, response.totalTopics());
                assertEquals(2, response.completedTopics());
                assertEquals(100.0, response.progressPercent());

                assertEquals(1, response.recentLearningPaths().size());
                assertEquals(
                                100.0,
                                response.recentLearningPaths().get(0).progressPercent());
        }

        @Test
        void getDashboardSummaryReturnsAtMostFiveRecentLearningPaths() {
                User user = User.builder()
                                .id(4L)
                                .email("paths@example.com")
                                .password("test-password")
                                .name("Paths User")
                                .build();

                List<Syllabus> syllabi = new ArrayList<>();

                for (long id = 6; id >= 1; id--) {
                        syllabi.add(Syllabus.builder()
                                        .id(id)
                                        .topic("Topic " + id)
                                        .user(user)
                                        .modules(new ArrayList<>())
                                        .build());
                }

                when(syllabusRepository.findByUserOrderByCreatedAtDesc(user))
                                .thenReturn(syllabi);
                when(quizAttemptRepository
                                .findByUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(4L))
                                .thenReturn(List.of());

                DashboardResponse response = syllabusService.getDashboardSummary(user);

                assertEquals(6, response.totalLearningPaths());
                assertEquals(5, response.recentLearningPaths().size());
                assertEquals(6L, response.recentLearningPaths().get(0).syllabusId());
                assertEquals(5L, response.recentLearningPaths().get(1).syllabusId());
                assertEquals(2L, response.recentLearningPaths().get(4).syllabusId());
        }

        @Test
        void getDashboardSummaryIncludesContinueLearningAndRecentTopicActivity() {
                User user = User.builder()
                                .id(5L)
                                .email("activity@example.com")
                                .password("test-password")
                                .name("Activity User")
                                .build();

                LocalDateTime completedAt = LocalDateTime.of(2026, 10, 31, 14, 30);
                LocalDateTime lastActiveAt = LocalDateTime.of(2026, 10, 31, 15, 0);

                Topic completedTopic = Topic.builder()
                                .id(501L)
                                .name("Java Variables")
                                .completed(true)
                                .completedAt(completedAt)
                                .build();

                Topic nextTopic = Topic.builder()
                                .id(502L)
                                .name("Java Methods")
                                .completed(false)
                                .build();

                SyllabusModule module = SyllabusModule.builder()
                                .id(50L)
                                .title("Java Fundamentals")
                                .estimatedHours(4.0)
                                .topics(new ArrayList<>(List.of(completedTopic, nextTopic)))
                                .build();

                Syllabus syllabus = Syllabus.builder()
                                .id(500L)
                                .topic("Java")
                                .description("Learn Java fundamentals")
                                .user(user)
                                .createdAt(LocalDateTime.of(2026, 10, 30, 10, 0))
                                .updatedAt(lastActiveAt)
                                .modules(new ArrayList<>(List.of(module)))
                                .build();

                when(syllabusRepository.findByUserOrderByCreatedAtDesc(user))
                                .thenReturn(List.of(syllabus));

                when(quizRepository.findByModuleId(50L))
                                .thenReturn(Optional.empty());

                when(quizAttemptRepository
                                .findByUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(5L))
                                .thenReturn(List.of());

                DashboardResponse response = syllabusService.getDashboardSummary(user);

                assertEquals(500L, response.continueLearning().syllabusId());
                assertEquals("Java", response.continueLearning().topic());
                assertEquals(50L, response.continueLearning().moduleId());
                assertEquals("Java Fundamentals", response.continueLearning().moduleTitle());
                assertEquals(502L, response.continueLearning().topicId());
                assertEquals("Java Methods", response.continueLearning().topicTitle());

                assertEquals(1, response.recentActivity().size());

                DashboardResponse.RecentActivity activity = response.recentActivity().get(0);

                assertEquals("TOPIC_COMPLETED", activity.type());
                assertEquals(500L, activity.syllabusId());
                assertEquals(50L, activity.moduleId());
                assertEquals(501L, activity.itemId());
                assertEquals("Java Variables", activity.title());
                assertEquals(completedAt, activity.occurredAt());
                assertEquals("COMPLETED", activity.status());

                DashboardResponse.LearningPathSummary summary = response.recentLearningPaths().get(0);

                assertEquals("Learn Java fundamentals", summary.description());
                assertEquals(4.0, summary.estimatedHours());
                assertEquals("IN_PROGRESS", summary.status());
                assertEquals(lastActiveAt, summary.lastActiveAt());
        }

        @Test
        void getDashboardSummaryOrdersTopicAndQuizActivityNewestFirst() {
                User user = User.builder()
                                .id(6L)
                                .email("ordered-activity@example.com")
                                .password("test-password")
                                .name("Ordered Activity User")
                                .build();

                LocalDateTime topicCompletedAt = LocalDateTime.of(2026, 10, 31, 14, 0);
                LocalDateTime quizSubmittedAt = LocalDateTime.of(2026, 10, 31, 15, 0);

                Topic completedTopic = Topic.builder()
                                .id(601L)
                                .name("Java Variables")
                                .completed(true)
                                .completedAt(topicCompletedAt)
                                .build();

                SyllabusModule module = SyllabusModule.builder()
                                .id(60L)
                                .title("Java Fundamentals")
                                .topics(new ArrayList<>(List.of(completedTopic)))
                                .build();

                Syllabus syllabus = Syllabus.builder()
                                .id(600L)
                                .topic("Java")
                                .user(user)
                                .modules(new ArrayList<>(List.of(module)))
                                .build();
                module.setSyllabus(syllabus);

                Quiz quiz = Quiz.builder()
                                .id(602L)
                                .title("Java Fundamentals Quiz")
                                .module(module)
                                .build();

                QuizAttempt attempt = QuizAttempt.builder()
                                .id(603L)
                                .quiz(quiz)
                                .passed(true)
                                .score(1)
                                .totalQuestions(1)
                                .userAnswersJson("[0]")
                                .submittedAt(quizSubmittedAt)
                                .build();

                when(syllabusRepository.findByUserOrderByCreatedAtDesc(user))
                                .thenReturn(List.of(syllabus));
                when(quizRepository.findByModuleId(60L))
                                .thenReturn(Optional.of(quiz));
                when(quizAttemptRepository.existsByQuizIdAndUserIdAndPassedTrue(602L, 6L))
                                .thenReturn(true);
                when(quizAttemptRepository
                                .findByUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(6L))
                                .thenReturn(List.of(attempt));

                DashboardResponse response = syllabusService.getDashboardSummary(user);

                assertEquals(2, response.recentActivity().size());
                assertEquals("QUIZ_SUBMITTED", response.recentActivity().get(0).type());
                assertEquals(quizSubmittedAt, response.recentActivity().get(0).occurredAt());
                assertEquals("TOPIC_COMPLETED", response.recentActivity().get(1).type());
                assertEquals(topicCompletedAt, response.recentActivity().get(1).occurredAt());
        }

        @Test
        void getDashboardSummaryReturnsAtMostTenRecentActivityEvents() {
                User user = User.builder()
                                .id(7L)
                                .email("many-events@example.com")
                                .password("test-password")
                                .name("Many Events User")
                                .build();

                List<Topic> topics = new ArrayList<>();
                for (long index = 0; index < 11; index++) {
                        topics.add(Topic.builder()
                                        .id(700L + index)
                                        .name("Topic " + index)
                                        .completed(true)
                                        .completedAt(LocalDateTime.of(2026, 10, 31, (int) index, 0))
                                        .build());
                }

                SyllabusModule module = SyllabusModule.builder()
                                .id(70L)
                                .title("Java Fundamentals")
                                .topics(topics)
                                .build();

                Syllabus syllabus = Syllabus.builder()
                                .id(700L)
                                .topic("Java")
                                .user(user)
                                .modules(new ArrayList<>(List.of(module)))
                                .build();

                when(syllabusRepository.findByUserOrderByCreatedAtDesc(user))
                                .thenReturn(List.of(syllabus));
                when(quizRepository.findByModuleId(70L))
                                .thenReturn(Optional.empty());
                when(quizAttemptRepository
                                .findByUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(7L))
                                .thenReturn(List.of());

                DashboardResponse response = syllabusService.getDashboardSummary(user);

                assertEquals(10, response.recentActivity().size());
        }

        @Test
        void getDashboardSummaryDoesNotCompletePathWhenModuleQuizIsUnpassed() {
                User user = User.builder()
                                .id(8L)
                                .email("unpassed-quiz@example.com")
                                .password("test-password")
                                .name("Unpassed Quiz User")
                                .build();

                LocalDateTime quizSubmittedAt = LocalDateTime.of(2026, 10, 31, 15, 0);
                Topic completedTopic = Topic.builder()
                                .id(801L)
                                .name("Java Variables")
                                .completed(true)
                                .build();

                SyllabusModule module = SyllabusModule.builder()
                                .id(80L)
                                .title("Java Fundamentals")
                                .topics(new ArrayList<>(List.of(completedTopic)))
                                .build();

                Syllabus syllabus = Syllabus.builder()
                                .id(800L)
                                .topic("Java")
                                .user(user)
                                .modules(new ArrayList<>(List.of(module)))
                                .build();
                module.setSyllabus(syllabus);

                Quiz quiz = Quiz.builder()
                                .id(802L)
                                .title("Java Fundamentals Quiz")
                                .module(module)
                                .build();
                QuizAttempt failedAttempt = QuizAttempt.builder()
                                .id(803L)
                                .quiz(quiz)
                                .passed(false)
                                .score(0)
                                .totalQuestions(1)
                                .userAnswersJson("[1]")
                                .submittedAt(quizSubmittedAt)
                                .build();

                when(syllabusRepository.findByUserOrderByCreatedAtDesc(user))
                                .thenReturn(List.of(syllabus));
                when(quizRepository.findByModuleId(80L))
                                .thenReturn(Optional.of(quiz));
                when(quizAttemptRepository.existsByQuizIdAndUserIdAndPassedTrue(802L, 8L))
                                .thenReturn(false);
                when(quizAttemptRepository
                                .findByUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(8L))
                                .thenReturn(List.of(failedAttempt));

                DashboardResponse response = syllabusService.getDashboardSummary(user);

                assertEquals("IN_PROGRESS", response.recentLearningPaths().get(0).status());
                assertEquals(0, response.completedModules());
        }

        @Test
        void getDashboardSummaryReturnsNullContinueLearningWhenNoPathHasNextTopic() {
                User user = User.builder()
                                .id(9L)
                                .email("no-next-topic@example.com")
                                .password("test-password")
                                .name("No Next Topic User")
                                .build();

                Syllabus firstSyllabus = Syllabus.builder()
                                .id(900L)
                                .topic("Java")
                                .user(user)
                                .modules(new ArrayList<>(List.of(SyllabusModule.builder()
                                                .id(90L)
                                                .title("Java Fundamentals")
                                                .topics(new ArrayList<>(List.of(Topic.builder()
                                                                .id(901L)
                                                                .name("Java Variables")
                                                                .completed(true)
                                                                .build())))
                                                .build())))
                                .build();

                Syllabus secondSyllabus = Syllabus.builder()
                                .id(910L)
                                .topic("Python")
                                .user(user)
                                .modules(new ArrayList<>(List.of(SyllabusModule.builder()
                                                .id(91L)
                                                .title("Python Fundamentals")
                                                .topics(new ArrayList<>(List.of(Topic.builder()
                                                                .id(911L)
                                                                .name("Python Variables")
                                                                .completed(true)
                                                                .build())))
                                                .build())))
                                .build();

                when(syllabusRepository.findByUserOrderByCreatedAtDesc(user))
                                .thenReturn(List.of(firstSyllabus, secondSyllabus));
                when(quizRepository.findByModuleId(90L))
                                .thenReturn(Optional.empty());
                when(quizRepository.findByModuleId(91L))
                                .thenReturn(Optional.empty());
                when(quizAttemptRepository
                                .findByUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(9L))
                                .thenReturn(List.of());

                DashboardResponse response = syllabusService.getDashboardSummary(user);

                assertNull(response.continueLearning());
        }

}
