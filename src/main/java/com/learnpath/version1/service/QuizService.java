
package com.learnpath.version1.service;

import com.learnpath.version1.dto.*;
import com.learnpath.version1.entities.*;
import com.learnpath.version1.exception.ResourceNotFoundException;
import com.learnpath.version1.repositories.ModuleRepository;
import com.learnpath.version1.repositories.QuizAttemptRepository;
import com.learnpath.version1.repositories.QuizRepository;
import com.learnpath.version1.utility.UserContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QuizService {

    private static final int DEFAULT_PASS_MARK_PERCENT = 70;
    private static final int REQUIRED_QUESTION_COUNT = 5;

    private final ModuleRepository moduleRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final UserContext userContext;

    public QuizService(
            ModuleRepository moduleRepository,
            QuizRepository quizRepository,
            QuizAttemptRepository attemptRepository,
            ChatClient.Builder chatClientBuilder,
            ObjectMapper objectMapper,
            UserContext userContext) {
        this.moduleRepository = moduleRepository;
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.userContext = userContext;
    }

    @Transactional
    public QuizResponse generateQuiz(Long moduleId) {
        User currentUser = userContext.getCurrentUser();

        SyllabusModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));

        if (!module.getSyllabus().getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "You do not have permission to access this module");
        }

        verifyModuleUnlocked(module, currentUser.getId());
        verifyAllTopicsCompleted(module,
                "Complete all topics in this module before accessing the quiz.");

        Optional<Quiz> existingQuiz = quizRepository.findByModuleId(moduleId);

        if (existingQuiz.isPresent()) {
            Quiz quiz = existingQuiz.get();
            validateQuizQuestions(quiz, "accessed");
            return toQuizResponse(quiz);
        }

        List<Topic> topics = module.getTopics();
        StringBuilder contentBuilder = new StringBuilder();

        for (Topic topic : topics) {
            if (topic.getContent() == null || topic.getContent().isBlank()) {
                throw new IllegalStateException(
                        "Topic '" + topic.getName()
                                + "' has no content. View Content first.");
            }

            contentBuilder.append("### ")
                    .append(topic.getName())
                    .append("\n")
                    .append(topic.getContent())
                    .append("\n\n");
        }

        String prompt = """
                You are an expert educator. Create exactly five multiple-choice
                questions using the learning content below.

                Each question must contain four non-empty options (A, B, C, D),
                the correctOptionIndex (0 for A, 1 for B, 2 for C, 3 for D),
                and an explanation that briefly explains why the correct answer
                is correct.

                Learning content:
                %s

                Return ONLY a JSON array in this format:
                [
                  {
                    "questionText": "Question text",
                    "optionA": "Option A",
                    "optionB": "Option B",
                    "optionC": "Option C",
                    "optionD": "Option D",
                    "correctOptionIndex": 0,
                    "explanation": "Explanation of the correct answer"
                  }
                ]

                Do not include Markdown fences or additional text.
                """.formatted(contentBuilder);

        String aiOutput = chatClient.prompt()
                .system("You are an expert quiz maker. Always respond with raw JSON.")
                .user(prompt)
                .call()
                .content();

        if (aiOutput == null || aiOutput.isBlank()) {
            throw new IllegalStateException("AI returned an empty quiz response.");
        }

        List<AiQuestion> aiQuestions;

        try {
            aiQuestions = objectMapper.readValue(
                    extractJson(aiOutput),
                    new TypeReference<List<AiQuestion>>() {});

            validateGeneratedQuestions(aiQuestions);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse or validate quiz JSON.", e);
        }

        Quiz quiz = Quiz.builder()
                .module(module)
                .title("Module Quiz")
                .description("Test your understanding of this module.")
                .passMarkPercent(DEFAULT_PASS_MARK_PERCENT)
                .build();

        List<QuizQuestion> questions = new ArrayList<>();

        for (AiQuestion aiQuestion : aiQuestions) {
            QuizQuestion question = QuizQuestion.builder()
                    .quiz(quiz)
                    .questionText(aiQuestion.questionText())
                    .optionA(aiQuestion.optionA())
                    .optionB(aiQuestion.optionB())
                    .optionC(aiQuestion.optionC())
                    .optionD(aiQuestion.optionD())
                    .correctOptionIndex(aiQuestion.correctOptionIndex())
                    .explanation(aiQuestion.explanation())
                    .build();

            questions.add(question);
        }

        quiz.setQuestions(questions);
        quizRepository.save(quiz);

        return toQuizResponse(quiz);
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuiz(Long quizId) {
        Quiz quiz = getAccessibleQuiz(quizId);
        validateQuizQuestions(quiz, "accessed");
        return toQuizResponse(quiz);
    }

    /**
     * Starts an attempt or returns the user's existing in-progress attempt.
     * Repeated start requests therefore do not create duplicate active attempts.
     */
    @Transactional
    public QuizAttemptStartResponse startAttempt(Long quizId) {
        User currentUser = userContext.getCurrentUser();
        Quiz quiz = getAccessibleQuiz(quizId);

        validateQuizQuestions(quiz, "started");

        Optional<QuizAttempt> activeAttempt =
                attemptRepository
                        .findFirstByQuizIdAndUserIdAndUserAnswersJsonIsNullOrderByStartedAtDesc(
                                quizId, currentUser.getId());

        if (activeAttempt.isPresent()) {
            QuizAttempt attempt = activeAttempt.get();

            if (attempt.getStartedAt() == null) {
                attempt.setStartedAt(
                        attempt.getAttemptedAt() != null
                                ? attempt.getAttemptedAt()
                                : LocalDateTime.now());
                attemptRepository.save(attempt);
            }

            return new QuizAttemptStartResponse(
                    attempt.getId(),
                    quiz.getId(),
                    attempt.getStartedAt());
        }

        LocalDateTime now = LocalDateTime.now();

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .user(currentUser)
                .score(0)
                .totalQuestions(quiz.getQuestions().size())
                .passed(false)
                .startedAt(now)
                .build();

        attemptRepository.save(attempt);

        return new QuizAttemptStartResponse(
                attempt.getId(),
                quiz.getId(),
                attempt.getStartedAt());
    }

    @Transactional
    public QuizResultResponse submitQuiz(
            Long quizId,
            QuizSubmissionRequest submission) {

        User currentUser = userContext.getCurrentUser();
        Quiz quiz = getAccessibleQuiz(quizId);
        validateQuizQuestions(quiz, "submitted");

        List<QuizQuestion> questions = quiz.getQuestions();

        if (submission == null || submission.answers() == null) {
            throw new IllegalArgumentException(
                    "Quiz submission and answers are required.");
        }

        if (submission.answers().size() != questions.size()) {
            throw new IllegalArgumentException(
                    "Number of answers does not match the number of questions.");
        }

        Map<Long, Integer> answersByQuestionId = new HashMap<>();

        for (QuizSubmissionRequest.Answer answer : submission.answers()) {
            if (answer == null
                    || answer.questionId() == null
                    || answer.selectedIndex() == null
                    || answer.selectedIndex() < 0
                    || answer.selectedIndex() > 3) {
                throw new IllegalArgumentException(
                        "Each answer must include a question ID and a valid option index from 0 to 3.");
            }

            boolean belongsToQuiz = questions.stream()
                    .anyMatch(q -> q.getId().equals(answer.questionId()));

            if (!belongsToQuiz) {
                throw new IllegalArgumentException(
                        "A submitted question does not belong to this quiz.");
            }

            if (answersByQuestionId.putIfAbsent(
                    answer.questionId(), answer.selectedIndex()) != null) {
                throw new IllegalArgumentException(
                        "Duplicate answers for the same question are not allowed.");
            }
        }

        if (answersByQuestionId.size() != questions.size()) {
            throw new IllegalArgumentException(
                    "An answer is required for every quiz question.");
        }

        /*
         * Update an active attempt when the start endpoint was used.
         * If the frontend has not yet adopted that endpoint, create a
         * completed attempt as a backwards-compatible fallback.
         */
        QuizAttempt attempt = attemptRepository
                .findFirstByQuizIdAndUserIdAndUserAnswersJsonIsNullOrderByStartedAtDesc(
                        quizId, currentUser.getId())
                .orElseGet(() -> QuizAttempt.builder()
                        .quiz(quiz)
                        .user(currentUser)
                        .startedAt(LocalDateTime.now())
                        .build());

        LocalDateTime submittedAt = LocalDateTime.now();
        LocalDateTime startedAt = attempt.getStartedAt();

        if (startedAt == null) {
            startedAt = attempt.getAttemptedAt() != null
                    ? attempt.getAttemptedAt()
                    : submittedAt;
        }

        long timeTakenSeconds = Math.max(
                0L, Duration.between(startedAt, submittedAt).getSeconds());

        int score = 0;
        List<Integer> orderedAnswers = new ArrayList<>();
        List<Integer> correctIndices = new ArrayList<>();
        List<QuizResultResponse.QuestionReview> review = new ArrayList<>();

        for (QuizQuestion question : questions) {
            int selectedAnswer = answersByQuestionId.get(question.getId());
            int correctAnswer = question.getCorrectOptionIndex();
            boolean isCorrect = selectedAnswer == correctAnswer;

            orderedAnswers.add(selectedAnswer);
            correctIndices.add(correctAnswer);

            if (isCorrect) {
                score++;
            }

            review.add(new QuizResultResponse.QuestionReview(
                    question.getId(),
                    question.getQuestionText(),
                    List.of(
                            question.getOptionA(),
                            question.getOptionB(),
                            question.getOptionC(),
                            question.getOptionD()),
                    selectedAnswer,
                    correctAnswer,
                    isCorrect,
                    question.getExplanation()));
        }

        int passMarkPercent = getPassMarkPercent(quiz);
        double scorePercent = score * 100.0 / questions.size();
        boolean passed = scorePercent >= passMarkPercent;

        attempt.setQuiz(quiz);
        attempt.setUser(currentUser);
        attempt.setScore(score);
        attempt.setTotalQuestions(questions.size());
        attempt.setPassed(passed);
        attempt.setStartedAt(startedAt);
        attempt.setSubmittedAt(submittedAt);
        attempt.setTimeTakenSeconds(timeTakenSeconds);
        attempt.setUserAnswersJson(
                objectMapper.valueToTree(orderedAnswers).toString());

        attemptRepository.save(attempt);

        return new QuizResultResponse(
                score,
                questions.size(),
                passed,
                correctIndices,
                review,
                passMarkPercent,
                timeTakenSeconds);
    }

    @Transactional(readOnly = true)
    public QuizResultResponse getQuizResult(Long quizId) {
        User currentUser = userContext.getCurrentUser();
        Quiz quiz = getAccessibleQuiz(quizId);

        QuizAttempt attempt = attemptRepository
                .findFirstByQuizIdAndUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(
                        quizId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No completed quiz attempt found for this user"));

        return buildResultResponse(quiz, attempt);
    }

    @Transactional
    public WeaknessAnalysisResponse analyzeWeaknesses(Long quizId) {
        User currentUser = userContext.getCurrentUser();
        Quiz quiz = getAccessibleQuiz(quizId);

        QuizAttempt attempt = attemptRepository
                .findFirstByQuizIdAndUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(
                        quizId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No completed quiz attempt found for this user"));

        List<QuizQuestion> questions = quiz.getQuestions();
        validateQuizQuestions(quiz, "analyzed");

        List<Integer> userAnswers = parseStoredAnswers(attempt, questions.size());
        StringBuilder wrongQuestionsInfo = new StringBuilder();

        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion question = questions.get(i);
            int selectedAnswer = userAnswers.get(i);

            if (selectedAnswer != question.getCorrectOptionIndex()) {
                wrongQuestionsInfo.append("Q")
                        .append(i + 1)
                        .append(": ")
                        .append(question.getQuestionText())
                        .append("\nYour Answer: ")
                        .append(optionLetter(selectedAnswer))
                        .append("\nCorrect Answer: ")
                        .append(optionLetter(question.getCorrectOptionIndex()))
                        .append("\n\n");
            }
        }

        if (wrongQuestionsInfo.isEmpty()) {
            return new WeaknessAnalysisResponse(List.of());
        }

        List<Topic> topics = quiz.getModule().getTopics();

        if (topics == null || topics.isEmpty()) {
            throw new IllegalStateException(
                    "This module has no topics for weakness analysis.");
        }

        StringBuilder topicsContent = new StringBuilder();

        for (Topic topic : topics) {
            if (topic.getContent() == null || topic.getContent().isBlank()) {
                throw new IllegalStateException(
                        "Topic '" + topic.getName()
                                + "' has no content for weakness analysis.");
            }

            topicsContent.append("### ")
                    .append(topic.getName())
                    .append("\n")
                    .append(topic.getContent())
                    .append("\n\n");
        }

        String prompt = """
                You are an expert educational analyst.

                Questions answered incorrectly:
                %s

                Learning content:
                %s

                Identify the weak topics or concepts the learner should review.
                For each entry provide:
                - topicName: short title of the weak concept
                - reason: why the learner struggled
                - suggestion: a specific action to improve

                Return ONLY a JSON array:
                [
                  {
                    "topicName": "...",
                    "reason": "...",
                    "suggestion": "..."
                  }
                ]

                Do not include Markdown fences or extra text.
                """.formatted(wrongQuestionsInfo, topicsContent);

        String aiOutput = chatClient.prompt()
                .system(
                        "You are an expert in analyzing learning gaps. Always respond with raw JSON.")
                .user(prompt)
                .call()
                .content();

        if (aiOutput == null || aiOutput.isBlank()) {
            throw new IllegalStateException(
                    "AI returned an empty weakness analysis response.");
        }

        try {
            List<WeakTopic> weakTopics = objectMapper.readValue(
                    extractJson(aiOutput),
                    new TypeReference<List<WeakTopic>>() {});

            if (weakTopics == null) {
                throw new IllegalArgumentException(
                        "AI returned a null weakness analysis.");
            }

            for (WeakTopic topic : weakTopics) {
                if (topic == null
                        || topic.topicName() == null
                        || topic.topicName().isBlank()
                        || topic.reason() == null
                        || topic.reason().isBlank()
                        || topic.suggestion() == null
                        || topic.suggestion().isBlank()) {
                    throw new IllegalArgumentException(
                            "AI returned an incomplete weakness analysis entry.");
                }
            }

            return new WeaknessAnalysisResponse(weakTopics);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse or validate weakness analysis JSON.", e);
        }
    }

    private Quiz getAccessibleQuiz(Long quizId) {
        User currentUser = userContext.getCurrentUser();

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));

        verifyQuizOwnership(quiz, currentUser);
        verifyModuleUnlocked(quiz.getModule(), currentUser.getId());
        verifyAllTopicsCompleted(
                quiz.getModule(),
                "Complete all topics in this module before accessing the quiz.");

        return quiz;
    }

    private void verifyAllTopicsCompleted(
            SyllabusModule module,
            String errorMessage) {

        List<Topic> topics = module.getTopics();

        if (topics == null
                || topics.isEmpty()
                || topics.stream().anyMatch(topic -> !topic.isCompleted())) {
            throw new IllegalStateException(errorMessage);
        }
    }

    private void validateQuizQuestions(Quiz quiz, String action) {
        List<QuizQuestion> questions = quiz.getQuestions();

        if (questions == null || questions.isEmpty()
                || questions.stream().anyMatch(q -> q == null)) {
            throw new IllegalStateException(
                    "This quiz has no valid questions and cannot be " + action + ".");
        }

        for (QuizQuestion question : questions) {
            if (question.getCorrectOptionIndex() < 0
                    || question.getCorrectOptionIndex() > 3
                    || isBlank(question.getQuestionText())
                    || isBlank(question.getOptionA())
                    || isBlank(question.getOptionB())
                    || isBlank(question.getOptionC())
                    || isBlank(question.getOptionD())) {
                throw new IllegalStateException(
                        "This quiz contains an invalid question and cannot be " + action + ".");
            }
        }
    }

    private void validateGeneratedQuestions(List<AiQuestion> questions) {
        if (questions == null || questions.size() != REQUIRED_QUESTION_COUNT) {
            throw new IllegalArgumentException(
                    "AI must generate exactly five quiz questions.");
        }

        for (AiQuestion question : questions) {
            if (question == null
                    || isBlank(question.questionText())
                    || isBlank(question.optionA())
                    || isBlank(question.optionB())
                    || isBlank(question.optionC())
                    || isBlank(question.optionD())
                    || question.correctOptionIndex() == null
                    || question.correctOptionIndex() < 0
                    || question.correctOptionIndex() > 3) {
                throw new IllegalArgumentException(
                        "AI generated an invalid quiz question or answer.");
            }
        }
    }

    private QuizResponse toQuizResponse(Quiz quiz) {
        List<QuestionDto> questionDtos = quiz.getQuestions().stream()
                .map(question -> new QuestionDto(
                        question.getId(),
                        question.getQuestionText(),
                        question.getOptionA(),
                        question.getOptionB(),
                        question.getOptionC(),
                        question.getOptionD()))
                .toList();

        return new QuizResponse(
                quiz.getId(),
                getQuizTitle(quiz),
                getQuizDescription(quiz),
                getPassMarkPercent(quiz),
                questionDtos);
    }

    private QuizResultResponse buildResultResponse(
            Quiz quiz,
            QuizAttempt attempt) {

        List<QuizQuestion> questions = quiz.getQuestions();
        validateQuizQuestions(quiz, "reviewed");

        List<Integer> userAnswers = parseStoredAnswers(attempt, questions.size());
        List<Integer> correctIndices = new ArrayList<>();
        List<QuizResultResponse.QuestionReview> review = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion question = questions.get(i);
            int selectedAnswer = userAnswers.get(i);
            int correctAnswer = question.getCorrectOptionIndex();

            correctIndices.add(correctAnswer);

            review.add(new QuizResultResponse.QuestionReview(
                    question.getId(),
                    question.getQuestionText(),
                    List.of(
                            question.getOptionA(),
                            question.getOptionB(),
                            question.getOptionC(),
                            question.getOptionD()),
                    selectedAnswer,
                    correctAnswer,
                    selectedAnswer == correctAnswer,
                    question.getExplanation()));
        }

        Long duration = attempt.getTimeTakenSeconds();

        return new QuizResultResponse(
                attempt.getScore(),
                attempt.getTotalQuestions(),
                attempt.isPassed(),
                correctIndices,
                review,
                getPassMarkPercent(quiz),
                duration);
    }

    private List<Integer> parseStoredAnswers(
            QuizAttempt attempt,
            int expectedQuestionCount) {

        if (attempt.getUserAnswersJson() == null
                || attempt.getUserAnswersJson().isBlank()) {
            throw new IllegalStateException(
                    "This quiz attempt has not been submitted.");
        }

        List<Integer> answers;

        try {
            answers = objectMapper.readValue(
                    attempt.getUserAnswersJson(),
                    new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to read the saved quiz answers.", e);
        }

        if (answers == null || answers.size() != expectedQuestionCount
                || attempt.getTotalQuestions() != expectedQuestionCount) {
            throw new IllegalStateException(
                    "Saved quiz answers do not match the quiz questions.");
        }

        for (Integer answer : answers) {
            if (answer == null || answer < 0 || answer > 3) {
                throw new IllegalStateException(
                        "Saved quiz data contains an invalid answer.");
            }
        }

        return answers;
    }

    private int getPassMarkPercent(Quiz quiz) {
        Integer passMark = quiz.getPassMarkPercent();

        if (passMark == null) {
            return DEFAULT_PASS_MARK_PERCENT;
        }

        if (passMark < 0 || passMark > 100) {
            throw new IllegalStateException(
                    "Quiz pass mark must be between 0 and 100.");
        }

        return passMark;
    }

    private String getQuizTitle(Quiz quiz) {
        return isBlank(quiz.getTitle()) ? "Module Quiz" : quiz.getTitle();
    }

    private String getQuizDescription(Quiz quiz) {
        return isBlank(quiz.getDescription())
                ? "Test your understanding of this module."
                : quiz.getDescription();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void verifyModuleUnlocked(SyllabusModule module, Long userId) {
        List<SyllabusModule> modules = module.getSyllabus().getModules();
        int currentIndex = -1;

        for (int i = 0; i < modules.size(); i++) {
            if (modules.get(i).getId().equals(module.getId())) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            throw new IllegalStateException(
                    "Module does not belong to the syllabus.");
        }

        if (currentIndex == 0) {
            return;
        }

        SyllabusModule previousModule = modules.get(currentIndex - 1);

        boolean previousModulePassed = quizRepository
                .findByModuleId(previousModule.getId())
                .map(Quiz::getId)
                .map(previousQuizId ->
                        attemptRepository.existsByQuizIdAndUserIdAndPassedTrue(
                                previousQuizId, userId))
                .orElse(false);

        if (!previousModulePassed) {
            throw new IllegalStateException(
                    "Complete the previous module and pass its quiz to unlock this module.");
        }
    }

    private void verifyQuizOwnership(Quiz quiz, User currentUser) {
        Long ownerId = quiz.getModule()
                .getSyllabus()
                .getUser()
                .getId();

        if (!ownerId.equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "You do not have permission to access this quiz");
        }
    }

    private String optionLetter(int index) {
        return switch (index) {
            case 0 -> "A";
            case 1 -> "B";
            case 2 -> "C";
            case 3 -> "D";
            default -> "?";
        };
    }

    private String extractJson(String raw) {
        String trimmed = raw.trim();

        if (trimmed.startsWith("```")) {
            String[] lines = trimmed.split("\n");

            if (lines.length >= 3) {
                StringBuilder builder = new StringBuilder();

                for (int i = 1; i < lines.length - 1; i++) {
                    builder.append(lines[i]);

                    if (i < lines.length - 2) {
                        builder.append("\n");
                    }
                }

                return builder.toString().trim();
            }
        }

        return trimmed;
    }

    private record AiQuestion(
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            Integer correctOptionIndex,
            String explanation) {}
}
