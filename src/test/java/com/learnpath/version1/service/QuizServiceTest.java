
package com.learnpath.version1.service;

import com.learnpath.version1.dto.QuizResultResponse;
import com.learnpath.version1.dto.QuizSubmissionRequest;
import com.learnpath.version1.entities.*;
import com.learnpath.version1.repositories.ModuleRepository;
import com.learnpath.version1.repositories.QuizAttemptRepository;
import com.learnpath.version1.repositories.QuizRepository;
import com.learnpath.version1.utility.UserContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuizServiceTest {

        private ModuleRepository moduleRepository;
        private QuizRepository quizRepository;
        private QuizAttemptRepository attemptRepository;
        private ChatClient.Builder chatClientBuilder;
        private ChatClient chatClient;
        private UserContext userContext;
        private QuizService quizService;

        private User user;
        private Quiz quiz;
        private List<QuizQuestion> questions;

        @BeforeEach
        void setUp() {
                moduleRepository = mock(ModuleRepository.class);
                quizRepository = mock(QuizRepository.class);
                attemptRepository = mock(QuizAttemptRepository.class);
                chatClientBuilder = mock(ChatClient.Builder.class);
                chatClient = mock(ChatClient.class);
                userContext = mock(UserContext.class);

                when(chatClientBuilder.build()).thenReturn(chatClient);

                quizService = new QuizService(
                                moduleRepository,
                                quizRepository,
                                attemptRepository,
                                chatClientBuilder,
                                new ObjectMapper(),
                                userContext);

                user = User.builder()
                                .id(10L)
                                .email("student@example.com")
                                .password("test-password")
                                .name("Test Student")
                                .build();

                Syllabus syllabus = Syllabus.builder()
                                .id(20L)
                                .user(user)
                                .modules(new ArrayList<>())
                                .build();

                SyllabusModule module = SyllabusModule.builder()
                                .id(30L)
                                .syllabus(syllabus)
                                .topics(new ArrayList<>())
                                .build();

                syllabus.getModules().add(module);

                quiz = Quiz.builder()
                                .id(40L)
                                .module(module)
                                .title("Test Quiz")
                                .description("Quiz for unit testing")
                                .passMarkPercent(70)
                                .questions(new ArrayList<>())
                                .build();

                questions = new ArrayList<>();

                for (long id = 101L; id <= 105L; id++) {
                        QuizQuestion question = QuizQuestion.builder()
                                        .id(id)
                                        .quiz(quiz)
                                        .questionText("Question " + id)
                                        .optionA("Option A")
                                        .optionB("Option B")
                                        .optionC("Option C")
                                        .optionD("Option D")
                                        .correctOptionIndex(0)
                                        .explanation("Explanation for question " + id)
                                        .build();

                        questions.add(question);
                }

                quiz.setQuestions(questions);

                when(userContext.getCurrentUser()).thenReturn(user);
                when(quizRepository.findById(40L)).thenReturn(Optional.of(quiz));

                // The first module is unlocked, and all topics must be completed.
                module.setTopics(List.of(
                                Topic.builder()
                                                .id(50L)
                                                .name("Completed topic")
                                                .content("Topic content")
                                                .completed(true)
                                                .syllabusModule(module)
                                                .build()));
        }

        private QuizSubmissionRequest validSubmission() {
                return new QuizSubmissionRequest(List.of(
                                new QuizSubmissionRequest.Answer(101L, 0),
                                new QuizSubmissionRequest.Answer(102L, 0),
                                new QuizSubmissionRequest.Answer(103L, 0),
                                new QuizSubmissionRequest.Answer(104L, 0),
                                new QuizSubmissionRequest.Answer(105L, 0)));
        }

        @Test
        void submitQuizRejectsNullSubmission() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> quizService.submitQuiz(40L, null));
        }

        @Test
        void submitQuizRejectsMissingAnswers() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> quizService.submitQuiz(
                                                40L,
                                                new QuizSubmissionRequest(null)));
        }

        @Test
        void submitQuizRejectsMissingQuestionAnswer() {
                QuizSubmissionRequest submission = new QuizSubmissionRequest(List.of(
                                new QuizSubmissionRequest.Answer(101L, 0),
                                new QuizSubmissionRequest.Answer(102L, 0),
                                new QuizSubmissionRequest.Answer(103L, 0),
                                new QuizSubmissionRequest.Answer(104L, 0)));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> quizService.submitQuiz(40L, submission));
        }

        @Test
        void submitQuizRejectsDuplicateQuestionIds() {
                QuizSubmissionRequest submission = new QuizSubmissionRequest(List.of(
                                new QuizSubmissionRequest.Answer(101L, 0),
                                new QuizSubmissionRequest.Answer(101L, 1),
                                new QuizSubmissionRequest.Answer(103L, 0),
                                new QuizSubmissionRequest.Answer(104L, 0),
                                new QuizSubmissionRequest.Answer(105L, 0)));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> quizService.submitQuiz(40L, submission));
        }

        @Test
        void submitQuizRejectsQuestionFromAnotherQuiz() {
                QuizSubmissionRequest submission = new QuizSubmissionRequest(List.of(
                                new QuizSubmissionRequest.Answer(999L, 0),
                                new QuizSubmissionRequest.Answer(102L, 0),
                                new QuizSubmissionRequest.Answer(103L, 0),
                                new QuizSubmissionRequest.Answer(104L, 0),
                                new QuizSubmissionRequest.Answer(105L, 0)));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> quizService.submitQuiz(40L, submission));
        }

        @Test
        void submitQuizRejectsOptionIndexOutsideZeroToThree() {
                QuizSubmissionRequest submission = new QuizSubmissionRequest(List.of(
                                new QuizSubmissionRequest.Answer(101L, 4),
                                new QuizSubmissionRequest.Answer(102L, 0),
                                new QuizSubmissionRequest.Answer(103L, 0),
                                new QuizSubmissionRequest.Answer(104L, 0),
                                new QuizSubmissionRequest.Answer(105L, 0)));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> quizService.submitQuiz(40L, submission));
        }

        @Test
        void submitQuizRejectsAnswerWithNullQuestionId() {
                QuizSubmissionRequest submission = new QuizSubmissionRequest(List.of(
                                new QuizSubmissionRequest.Answer(null, 0),
                                new QuizSubmissionRequest.Answer(102L, 0),
                                new QuizSubmissionRequest.Answer(103L, 0),
                                new QuizSubmissionRequest.Answer(104L, 0),
                                new QuizSubmissionRequest.Answer(105L, 0)));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> quizService.submitQuiz(40L, submission));
        }

        @Test
        void submitQuizRejectsAnswerWithNullOptionIndex() {
                QuizSubmissionRequest submission = new QuizSubmissionRequest(List.of(
                                new QuizSubmissionRequest.Answer(101L, null),
                                new QuizSubmissionRequest.Answer(102L, 0),
                                new QuizSubmissionRequest.Answer(103L, 0),
                                new QuizSubmissionRequest.Answer(104L, 0),
                                new QuizSubmissionRequest.Answer(105L, 0)));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> quizService.submitQuiz(40L, submission));
        }

        @Test
        void submitQuizRejectsAccessToAnotherUsersQuiz() {
                User anotherUser = User.builder()
                                .id(999L)
                                .email("other@example.com")
                                .password("test-password")
                                .build();

                when(userContext.getCurrentUser()).thenReturn(anotherUser);

                assertThrows(
                                AccessDeniedException.class,
                                () -> quizService.submitQuiz(40L, validSubmission()));

                verify(attemptRepository, never()).save(any(QuizAttempt.class));
        }

        @Test
        void submitQuizReturnsPerfectScoreWhenAllAnswersAreCorrect() {
                when(attemptRepository.save(any(QuizAttempt.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                QuizResultResponse result = quizService.submitQuiz(40L, validSubmission());

                assertEquals(5, result.score());
                assertEquals(5, result.totalQuestions());
                assertTrue(result.passed());
                assertEquals(70, result.passMarkPercent());
        }

        @Test
        void submitQuizFailsWhenNoAnswersAreCorrect() {
                when(attemptRepository.save(any(QuizAttempt.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                QuizSubmissionRequest submission = new QuizSubmissionRequest(
                                List.of(
                                                new QuizSubmissionRequest.Answer(101L, 1),
                                                new QuizSubmissionRequest.Answer(102L, 1),
                                                new QuizSubmissionRequest.Answer(103L, 1),
                                                new QuizSubmissionRequest.Answer(104L, 1),
                                                new QuizSubmissionRequest.Answer(105L, 1)));

                QuizResultResponse result = quizService.submitQuiz(40L, submission);

                assertEquals(0, result.score());
                assertFalse(result.passed());
        }

        @Test
        void submitQuizIncludesExplanationsInResultReview() {
                when(attemptRepository.save(any(QuizAttempt.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                QuizResultResponse result = quizService.submitQuiz(40L, validSubmission());

                assertNotNull(result.review());
                assertEquals(5, result.review().size());

                assertTrue(
                                result.review().stream()
                                                .allMatch(question -> question.explanation() != null
                                                                && !question.explanation().isBlank()));
        }

        @Test
        void startAttemptCreatesAttemptWithStartTime() {
                when(attemptRepository.save(any(QuizAttempt.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                var response = quizService.startAttempt(40L);

                assertNotNull(response);

                var captor = org.mockito.ArgumentCaptor.forClass(QuizAttempt.class);

                verify(attemptRepository).save(captor.capture());

                QuizAttempt savedAttempt = captor.getValue();

                assertNotNull(savedAttempt.getStartedAt());
                assertNull(savedAttempt.getSubmittedAt());
                assertNull(savedAttempt.getUserAnswersJson());
        }

        @Test
        void startAttemptReusesExistingActiveAttempt() {
                QuizAttempt activeAttempt = QuizAttempt.builder()
                                .quiz(quiz)
                                .user(user)
                                .startedAt(java.time.LocalDateTime.now().minusMinutes(2))
                                .build();

                when(attemptRepository
                                .findFirstByQuizIdAndUserIdAndUserAnswersJsonIsNullOrderByStartedAtDesc(
                                                40L, 10L))
                                .thenReturn(Optional.of(activeAttempt));

                var response = quizService.startAttempt(40L);

                assertNotNull(response);

                verify(attemptRepository, never()).save(any(QuizAttempt.class));
        }

        @Test
        void submitQuizRecordsAttemptCompletionTiming() {
                when(attemptRepository.save(any(QuizAttempt.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                quizService.submitQuiz(40L, validSubmission());

                var captor = org.mockito.ArgumentCaptor.forClass(QuizAttempt.class);

                verify(attemptRepository).save(captor.capture());

                QuizAttempt savedAttempt = captor.getValue();

                assertNotNull(savedAttempt.getStartedAt());
                assertNotNull(savedAttempt.getSubmittedAt());
                assertNotNull(savedAttempt.getTimeTakenSeconds());

                assertFalse(savedAttempt.getSubmittedAt()
                                .isBefore(savedAttempt.getStartedAt()));

                assertTrue(savedAttempt.getTimeTakenSeconds() >= 0);
                assertNotNull(savedAttempt.getUserAnswersJson());
        }

        @Test
        void submitQuizFailsWhenScoreIsBelowPassThreshold() {
                when(attemptRepository.save(any(QuizAttempt.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                QuizSubmissionRequest submission = new QuizSubmissionRequest(
                                List.of(
                                                new QuizSubmissionRequest.Answer(101L, 0), // Correct
                                                new QuizSubmissionRequest.Answer(102L, 0), // Correct
                                                new QuizSubmissionRequest.Answer(103L, 0), // Correct
                                                new QuizSubmissionRequest.Answer(104L, 1), // Incorrect
                                                new QuizSubmissionRequest.Answer(105L, 1) // Incorrect
                                ));

                QuizResultResponse result = quizService.submitQuiz(40L, submission);

                assertEquals(3, result.score());
                assertEquals(5, result.totalQuestions());
                assertEquals(70, result.passMarkPercent());
                assertFalse(result.passed());
        }

        @Test
        void getQuizAttemptHistoryReturnsCompletedAttemptsInRepositoryOrder() {
                QuizAttempt newest = QuizAttempt.builder()
                                .id(502L)
                                .quiz(quiz)
                                .user(user)
                                .score(4)
                                .totalQuestions(5)
                                .passed(true)
                                .userAnswersJson("[0,0,0,0,1]")
                                .startedAt(java.time.LocalDateTime.now().minusMinutes(2))
                                .submittedAt(java.time.LocalDateTime.now())
                                .timeTakenSeconds(120L)
                                .build();

                QuizAttempt older = QuizAttempt.builder()
                                .id(501L)
                                .quiz(quiz)
                                .user(user)
                                .score(2)
                                .totalQuestions(5)
                                .passed(false)
                                .userAnswersJson("[0,1,1,1,1]")
                                .timeTakenSeconds(90L)
                                .build();

                when(attemptRepository
                                .findByQuizIdAndUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(
                                                40L, 10L))
                                .thenReturn(List.of(newest, older));

                var history = quizService.getQuizAttemptHistory(40L);

                assertEquals(2, history.size());
                assertEquals(502L, history.get(0).attemptId());
                assertEquals(4, history.get(0).score());
                assertTrue(history.get(0).passed());
                assertEquals(120L, history.get(0).timeTakenSeconds());

                assertEquals(501L, history.get(1).attemptId());
                assertEquals(2, history.get(1).score());
                assertFalse(history.get(1).passed());
        }

        @Test
        void analyzeWeaknessesReturnsEmptyListForPerfectScore() {
                QuizAttempt perfectAttempt = QuizAttempt.builder()
                                .id(601L)
                                .quiz(quiz)
                                .user(user)
                                .score(5)
                                .totalQuestions(5)
                                .passed(true)
                                .userAnswersJson("[0,0,0,0,0]")
                                .build();

                when(attemptRepository
                                .findFirstByQuizIdAndUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(
                                                40L, 10L))
                                .thenReturn(Optional.of(perfectAttempt));

                var response = quizService.analyzeWeaknesses(40L);

                assertNotNull(response);
                assertNotNull(response.weakTopics());
                assertTrue(response.weakTopics().isEmpty());
        }

        @Test
        void analyzeWeaknessesRejectsWhenNoCompletedAttemptExists() {
                when(attemptRepository
                                .findFirstByQuizIdAndUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(
                                                40L, 10L))
                                .thenReturn(Optional.empty());

                assertThrows(
                                com.learnpath.version1.exception.ResourceNotFoundException.class,
                                () -> quizService.analyzeWeaknesses(40L));
        }

        @Test
        void getQuizRejectsQuizWithoutModule() {
                quiz.setModule(null);

                assertThrows(
                                IllegalStateException.class,
                                () -> quizService.getQuiz(40L));
        }

        @Test
        void getQuizRejectsInvalidPreviousModuleInSequence() {
                Syllabus syllabus = quiz.getModule().getSyllabus();

                List<SyllabusModule> invalidSequence = new ArrayList<>();
                invalidSequence.add(null);
                invalidSequence.add(quiz.getModule());

                syllabus.setModules(invalidSequence);

                assertThrows(
                                IllegalStateException.class,
                                () -> quizService.getQuiz(40L));
        }

        @Test
        void generateQuizRejectsModuleWithoutValidOwnerRelationship() {
                SyllabusModule module = quiz.getModule();
                module.setSyllabus(null);

                when(moduleRepository.findById(30L))
                                .thenReturn(Optional.of(module));

                assertThrows(
                                IllegalStateException.class,
                                () -> quizService.generateQuiz(30L));
        }

        @Test
        void getQuizRejectsAccessByAnotherUser() {
                User anotherUser = User.builder()
                                .id(999L)
                                .email("other@example.com")
                                .password("test-password")
                                .build();

                when(userContext.getCurrentUser()).thenReturn(anotherUser);

                assertThrows(
                                AccessDeniedException.class,
                                () -> quizService.getQuiz(40L));
        }

}
