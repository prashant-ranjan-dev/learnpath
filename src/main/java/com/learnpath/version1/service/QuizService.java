package com.learnpath.version1.service;

import com.learnpath.version1.dto.*;
import com.learnpath.version1.entities.*;
import com.learnpath.version1.exception.ResourceNotFoundException;
import com.learnpath.version1.repositories.ModuleRepository;
import com.learnpath.version1.repositories.QuizAttemptRepository;
import com.learnpath.version1.repositories.QuizRepository;
import com.learnpath.version1.repositories.TopicRepository;
import com.learnpath.version1.utility.UserContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class QuizService {

    private final ModuleRepository moduleRepository;
    private final TopicRepository topicRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final UserContext userContext;

    public QuizService(ModuleRepository moduleRepository,
            TopicRepository topicRepository,
            QuizRepository quizRepository,
            QuizAttemptRepository attemptRepository,
            ChatClient.Builder chatClientBuilder,
            ObjectMapper objectMapper,
            UserContext userContext) {
        this.moduleRepository = moduleRepository;
        this.topicRepository = topicRepository;
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.userContext = userContext;
    }
    /*
     * public QuizResponse generateQuiz(Long moduleId) {
     * // temporary test: return hardcoded quiz
     * List<QuestionDto> questions = List.of(
     * new QuestionDto(1L, "Test Q", "A", "B", "C", "D")
     * );
     * return new QuizResponse(1L, questions);
     * }
     */

    @Transactional
    public QuizResponse generateQuiz(Long moduleId) {
        User currentUser = userContext.getCurrentUser();

        SyllabusModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));

        if (!module.getSyllabus().getUser().getId()
                .equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "You do not have permission to access this module");
        }

        // A quiz is available only after every topic is completed.
        List<Topic> topics = module.getTopics();

        if (topics == null || topics.isEmpty()) {
            throw new IllegalStateException(
                    "This module has no topics and cannot be quizzed.");
        }

        boolean allTopicsCompleted = topics.stream()
                .allMatch(Topic::isCompleted);

        if (!allTopicsCompleted) {
            throw new IllegalStateException(
                    "Complete all topics in this module before accessing the quiz.");
        }

        // Prevent duplicate AI generation and database constraints
        Optional<Quiz> existingQuiz = quizRepository.findByModuleId(moduleId);
        if (existingQuiz.isPresent()) {
            Quiz quiz = existingQuiz.get();
            List<QuestionDto> questionDtos = quiz.getQuestions().stream()
                    .map(q -> new QuestionDto(q.getId(), q.getQuestionText(),
                            q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()))
                    .toList();
            return new QuizResponse(quiz.getId(), questionDtos);
        }

        // Collect content of all topics in this module
        List<Topic> topicsList = module.getTopics();
        StringBuilder contentBuilder = new StringBuilder();
        for (Topic t : topicsList) {
            if (t.getContent() == null || t.getContent().isBlank()) {
                throw new IllegalStateException("Topic '" + t.getName() + "' has no content. View Content first.");
            }
            contentBuilder.append("### ").append(t.getName()).append("\n").append(t.getContent()).append("\n\n");
        }

        String prompt = """
                You are an expert educator. Using the learning content below, create a 5-question multiple-choice quiz.
                            Each question must have 4 options (A, B, C, D) and the index of the correct answer (0 for A, 1 for B, etc.).

                            Content:
                            %s

                            Return ONLY a JSON array in the following format:
                            [
                              {
                                "questionText": "What is ...?",
                                "optionA": "...",
                                "optionB": "...",
                                "optionC": "...",
                                "optionD": "...",
                                "correctOptionIndex": 0
                              }
                            ]
                            Do not include any markdown fences or extra text.
                """
                .formatted(contentBuilder.toString());

        String aiOutput = chatClient.prompt()
                .system("You are an expert quiz maker. Always respond with raw JSON.")
                .user(prompt)
                .call()
                .content();

        System.out.println("AI quiz response received for module ID: " + moduleId);
        List<AiQuestion> aiQuestions;
        try {
            String json = extractJson(aiOutput);
            aiQuestions = objectMapper.readValue(
                    json,
                    new TypeReference<List<AiQuestion>>() {
                    });

            if (aiQuestions == null || aiQuestions.size() != 5) {
                throw new IllegalArgumentException(
                        "AI must generate exactly 5 quiz questions.");
            }

            for (AiQuestion question : aiQuestions) {
                if (question == null
                        || question.questionText() == null
                        || question.questionText().isBlank()
                        || question.optionA() == null || question.optionA().isBlank()
                        || question.optionB() == null || question.optionB().isBlank()
                        || question.optionC() == null || question.optionC().isBlank()
                        || question.optionD() == null || question.optionD().isBlank()
                        || question.correctOptionIndex() == null
                        || question.correctOptionIndex() < 0
                        || question.correctOptionIndex() > 3) {
                    throw new IllegalArgumentException(
                            "AI generated an invalid quiz question or answer.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse or validate quiz JSON.", e);
        }

        Quiz quiz = Quiz.builder().module(module).build();
        List<QuizQuestion> questions = new ArrayList<>();

        for (AiQuestion aiQ : aiQuestions) {
            QuizQuestion q = QuizQuestion.builder()
                    .quiz(quiz)
                    .questionText(aiQ.questionText())
                    .optionA(aiQ.optionA())
                    .optionB(aiQ.optionB())
                    .optionC(aiQ.optionC())
                    .optionD(aiQ.optionD())
                    .correctOptionIndex(aiQ.correctOptionIndex()) // CRITICAL FIX
                    .build();
            questions.add(q);
        }

        quiz.setQuestions(questions);
        quizRepository.save(quiz);

        List<QuestionDto> questionDtos = questions.stream()
                .map(q -> new QuestionDto(q.getId(), q.getQuestionText(),
                        q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()))
                .toList();

        return new QuizResponse(quiz.getId(), questionDtos);
    }

    @Transactional
    public QuizResultResponse submitQuiz(Long quizId, QuizSubmissionRequest submission) {
        User currentUser = userContext.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not Found"));

        verifyQuizOwnership(quiz, currentUser);

        SyllabusModule module = quiz.getModule();
        List<Topic> moduleTopics = module.getTopics();

        if (moduleTopics == null
                || moduleTopics.isEmpty()
                || moduleTopics.stream().anyMatch(topic -> !topic.isCompleted())) {
            throw new IllegalStateException(
                    "Complete all topics in this module before submitting the quiz.");
        }

        List<QuizQuestion> questions = quiz.getQuestions();

        if (questions == null || questions.isEmpty()) {
            throw new IllegalStateException(
                    "This quiz has no questions and cannot be submitted.");
        }

        if (submission == null || submission.answers() == null) {
            throw new IllegalArgumentException(
                    "Quiz submission and answers are required.");
        }

        if (questions.size() != submission.answers().size()) {
            throw new IllegalArgumentException(
                    "Number of answers does not match the number of questions.");
        }

        for (Integer answer : submission.answers()) {
            if (answer == null || answer < 0 || answer > 3) {
                throw new IllegalArgumentException(
                        "Each answer must be a valid option index from 0 to 3.");
            }
        }

        int score = 0;
        List<Integer> correctIndices = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            int correct = questions.get(i).getCorrectOptionIndex();
            correctIndices.add(correct);
            if (submission.answers().get(i) == correct) {
                score++;
            }
        }

        boolean passed = (score * 100.0 / questions.size()) > 70.0;

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .user(currentUser)
                .score(score)
                .totalQuestions(questions.size())
                .passed(passed)
                .userAnswersJson(objectMapper.valueToTree(submission.answers()).toString())
                .build();
        attemptRepository.save(attempt);

        return new QuizResultResponse(score, questions.size(), passed, correctIndices);
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

    private record AiQuestion(
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            Integer correctOptionIndex) {
    }

    @Transactional
    public WeaknessAnalysisResponse analyzeWeaknesses(Long quizId) {
        User currentUser = userContext.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not Found"));

        verifyQuizOwnership(quiz, currentUser);

        QuizAttempt attempt = attemptRepository
                .findFirstByQuizIdAndUserIdOrderByAttemptedAtDesc(quizId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No quiz attempt found for this user"));

        List<QuizQuestion> questions = quiz.getQuestions();

        if (questions == null || questions.isEmpty()) {
            throw new IllegalStateException(
                    "This quiz has no questions for weakness analysis.");
        }

        List<Integer> userAnswers;
        try {
            userAnswers = objectMapper.readValue(attempt.getUserAnswersJson(),
                    new TypeReference<List<Integer>>() {
                    });
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse user answers", e);
        }

        if (userAnswers == null || userAnswers.size() != questions.size()) {
            throw new IllegalStateException(
                    "Stored quiz answers do not match the quiz questions.");
        }

        for (Integer answer : userAnswers) {
            if (answer == null || answer < 0 || answer > 3) {
                throw new IllegalStateException(
                        "Stored quiz answers contain an invalid option index.");
            }
        }

        StringBuilder wrongQuestionsInfo = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            if (userAnswers.get(i) != q.getCorrectOptionIndex()) {
                wrongQuestionsInfo.append("Q").append(i + 1)
                        .append(": ").append(q.getQuestionText()).append("\n")
                        .append("Your Answer: ").append(optionLetter(userAnswers.get(i))).append("\n")
                        .append("Correct Answer: ").append(optionLetter(q.getCorrectOptionIndex())).append("\n\n");
            }
        }

        if (wrongQuestionsInfo.isEmpty()) {
            return new WeaknessAnalysisResponse(List.of());
        }

        SyllabusModule module = quiz.getModule();
        List<Topic> analysisTopics = module.getTopics();

        if (analysisTopics == null || analysisTopics.isEmpty()) {
            throw new IllegalStateException(
                    "This module has no topics for weakness analysis.");
        }

        StringBuilder topicsContent = new StringBuilder();
        for (Topic t : analysisTopics) {
            topicsContent.append("### ").append(t.getName()).append("\n");
            if (t.getContent() != null && !t.getContent().isBlank()) {
                topicsContent.append(t.getContent()).append("\n\n");
            }
        }

        String prompt = """
                You are an expert educational analyst. A learner has completed a quiz. Here are the questions they answered incorrectly:
                            %s

                            Here is the content of the topics in this module:
                            %s

                            Based on the learner's mistakes and the topic content, identify the weak topics/concepts that the learner should review. For each weak topic, provide:
                            - topicName: a short title of the weak topic/concept
                            - reason: why the learner struggled (based on the incorrect answers)
                            - suggestion: a specific action or resource to improve (e.g., "Review the section on X and practice exercises on Y")

                            Return ONLY a JSON array in the following format:
                            [
                              {
                                "topicName": "...",
                                "reason": "...",
                                "suggestion": "..."
                              }
                            ]
                            Do not include any markdown fences or extra text.
                """
                .formatted(wrongQuestionsInfo.toString(), topicsContent.toString());

        String aiOutput = chatClient.prompt()
                .system("You are an expert in analyzing learning gaps. Always respond with raw JSON.")
                .user(prompt)
                .call()
                .content();

        if (aiOutput == null || aiOutput.isBlank()) {
            throw new IllegalStateException(
                    "AI returned an empty weakness analysis response.");
        }

        List<WeakTopic> weakTopics;
        try {
            String json = extractJson(aiOutput);
            weakTopics = objectMapper.readValue(
                    json,
                    new TypeReference<List<WeakTopic>>() {
                    });

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
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse or validate weakness analysis JSON.", e);
        }

        return new WeaknessAnalysisResponse(weakTopics);
    }

    private String optionLetter(int idx) {
        return switch (idx) {
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
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < lines.length - 1; i++) {
                    sb.append(lines[i]);
                    if (i < lines.length - 2)
                        sb.append("\n");
                }
                return sb.toString().trim();
            }
        }
        return trimmed;
    }

}
