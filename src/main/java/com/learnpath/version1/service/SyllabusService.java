package com.learnpath.version1.service;

import com.learnpath.version1.dto.ModuleDto;
import com.learnpath.version1.dto.SyllabusRequest;
import com.learnpath.version1.dto.SyllabusResponse;
import com.learnpath.version1.dto.TopicDto;
import com.learnpath.version1.dto.DashboardResponse;
import com.learnpath.version1.entities.QuizAttempt;
import com.learnpath.version1.entities.Syllabus;
import com.learnpath.version1.entities.SyllabusModule;
import com.learnpath.version1.entities.Topic;
import com.learnpath.version1.entities.User;
import com.learnpath.version1.entities.Quiz;
import com.learnpath.version1.exception.AiResponseParsingException;
import com.learnpath.version1.exception.ResourceNotFoundException;
import com.learnpath.version1.repositories.SyllabusRepository;
import com.learnpath.version1.repositories.QuizAttemptRepository;
import com.learnpath.version1.repositories.QuizRepository;
import com.learnpath.version1.utility.UserContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

@Service
public class SyllabusService {

        record AiSyllabusResponse(List<AiModule> modules) {
        }

        record AiModule(
                        String title,
                        String description,
                        double estimatedHours,
                        List<AiTopic> topics) {
        }

        record AiTopic(
                        String name,
                        String description,
                        int estimatedMinutes) {
        }

        private final ChatClient chatClient;
        private final SyllabusRepository syllabusRepository;
        private final ObjectMapper objectMapper;
        private final UserContext userContext;
        private final QuizRepository quizRepository;
        private final QuizAttemptRepository quizAttemptRepository;

        public SyllabusService(
                        ChatClient.Builder chatClientBuilder,
                        SyllabusRepository syllabusRepository,
                        ObjectMapper objectMapper,
                        UserContext userContext,
                        QuizRepository quizRepository,
                        QuizAttemptRepository quizAttemptRepository) {

                this.chatClient = chatClientBuilder.build();
                this.syllabusRepository = syllabusRepository;
                this.objectMapper = objectMapper;
                this.userContext = userContext;
                this.quizRepository = quizRepository;
                this.quizAttemptRepository = quizAttemptRepository;
        }

        private Syllabus.Depth mapDepthLevel(String depthLevel) {
                return switch (depthLevel.trim().toLowerCase()) {
                        case "foundations" -> Syllabus.Depth.LEVEL1;
                        case "proficient" -> Syllabus.Depth.LEVEL3;
                        case "mastery" -> Syllabus.Depth.LEVEL5;

                        // Backward compatibility for existing clients.
                        case "level1" -> Syllabus.Depth.LEVEL1;
                        case "level2" -> Syllabus.Depth.LEVEL2;
                        case "level3" -> Syllabus.Depth.LEVEL3;
                        case "level4" -> Syllabus.Depth.LEVEL4;
                        case "level5" -> Syllabus.Depth.LEVEL5;

                        default -> throw new IllegalArgumentException(
                                        "Invalid depth level. Use Foundations, Proficient, or Mastery.");
                };
        }

        private Syllabus.CurrentUnderstanding mapCurrentUnderstanding(String currentUnderstanding) {
                if (currentUnderstanding == null || currentUnderstanding.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Current understanding is required. Use Beginner, Intermediate, or Pro.");
                }

                return switch (currentUnderstanding.trim().toLowerCase()) {
                        case "beginner" -> Syllabus.CurrentUnderstanding.BEGINNER;
                        case "intermediate" -> Syllabus.CurrentUnderstanding.INTERMEDIATE;
                        case "pro", "expert" -> Syllabus.CurrentUnderstanding.EXPERT;

                        // Backward compatibility for existing API clients.
                        case "beginner_level" -> Syllabus.CurrentUnderstanding.BEGINNER;
                        case "intermediate_level" -> Syllabus.CurrentUnderstanding.INTERMEDIATE;

                        default -> throw new IllegalArgumentException(
                                        "Invalid current understanding. Use Beginner, Intermediate, or Pro.");
                };
        }

        private void validateAiSyllabus(AiSyllabusResponse response) {
                if (response == null
                                || response.modules() == null
                                || response.modules().size() < 4
                                || response.modules().size() > 6) {
                        throw new AiResponseParsingException(
                                        "AI syllabus must contain between 4 and 6 modules.",
                                        null);
                }

                for (AiModule module : response.modules()) {
                        if (module == null
                                        || module.title() == null
                                        || module.title().isBlank()
                                        || module.description() == null
                                        || module.description().isBlank()) {
                                throw new AiResponseParsingException(
                                                "Each AI-generated module must have a title and description.",
                                                null);
                        }

                        if (!Double.isFinite(module.estimatedHours())
                                        || module.estimatedHours() <= 0) {
                                throw new AiResponseParsingException(
                                                "Each module must have a positive, finite estimated duration.",
                                                null);
                        }

                        if (module.topics() == null
                                        || module.topics().size() < 3
                                        || module.topics().size() > 5) {
                                throw new AiResponseParsingException(
                                                "Each module must contain between 3 and 5 topics.",
                                                null);
                        }

                        for (AiTopic topic : module.topics()) {
                                if (topic == null
                                                || topic.name() == null
                                                || topic.name().isBlank()
                                                || topic.description() == null
                                                || topic.description().isBlank()
                                                || topic.estimatedMinutes() <= 0) {
                                        throw new AiResponseParsingException(
                                                        "Each AI-generated topic must have a name and description.",
                                                        null);
                                }
                        }
                }
        }

        @Transactional
        public SyllabusResponse generateSyllabus(SyllabusRequest request) {
                // Build the prompt
                String prompt = buildPrompt(request);

                // Call the AI agent using the Google Gemini
                String aiOutput = chatClient.prompt()
                                .system("You are an expert curriculum designer. You create detailed, structured syllabi for learners. \n"
                                                +
                                                "Always respond in perfect JSON matching the user's requested schema. Do not include any extra text, markdown, or code fences.")
                                .user(prompt)
                                .call()
                                .content();

                // Validate the AI response before attempting JSON extraction.
                if (aiOutput == null || aiOutput.isBlank()) {
                        throw new AiResponseParsingException(
                                        "AI returned an empty syllabus response. Please try again.",
                                        null);
                }

                // Extract the Json from the AI Response
                String json = extractJson(aiOutput);
                AiSyllabusResponse aiResponse;
                try {
                        aiResponse = objectMapper.readValue(json, AiSyllabusResponse.class);
                } catch (JacksonException e) {
                        throw new AiResponseParsingException(
                                        "Failed to parse AI-generated syllabus. Please try again.", e);

                }

                validateAiSyllabus(aiResponse);

                Syllabus syllabus = Syllabus.builder()
                                .topic(request.topic())
                                .currentUnderstanding(mapCurrentUnderstanding(request.currentUnderstanding()))
                                .depthLevel(mapDepthLevel(request.depthLevel()))
                                .goal(request.goal())
                                .description("A learning path for " + request.topic()
                                                + ", designed to help you " + request.goal() + ".")
                                .user(userContext.getCurrentUser())
                                .modules(new ArrayList<>())
                                .build();

                List<SyllabusModule> moduleEntities = new ArrayList<>();

                for (AiModule aiMod : aiResponse.modules()) {
                        SyllabusModule mod = SyllabusModule.builder()
                                        .title(aiMod.title())
                                        .estimatedHours((float) aiMod.estimatedHours())
                                        .syllabus(syllabus)
                                        .description(aiMod.description())
                                        .topics(new ArrayList<>())
                                        .build();

                        for (AiTopic aiTopic : aiMod.topics()) {
                                Topic topic = Topic.builder()
                                                .name(aiTopic.name())
                                                .description(aiTopic.description())
                                                .estimatedMinutes(aiTopic.estimatedMinutes())
                                                .completed(false)
                                                .syllabusModule(mod)
                                                .build();

                                mod.getTopics().add(topic);
                        }
                        moduleEntities.add(mod);
                }
                syllabus.setModules(moduleEntities);

                Syllabus savedSyllabus = syllabusRepository.save(syllabus);

                return toResponseWithIds(savedSyllabus);
        }

        private String buildPrompt(SyllabusRequest request) {
                return """
                                Topic: %s
                                Current understanding: %s
                                Desired depth: %s
                                Goal: %s

                                Produce exactly 4-6 modules. Each module must have:
                                        - "title": a descriptive module title
                                        - "description": 1-2 sentences explaining what the learner will understand or be able to do after this module
                                        - "topics": a list of 3-5 topic objects, each containing:
                                                - "name": a specific topic name
                                                - "description": 1 concise sentence explaining what the topic covers
                                                - "estimatedMinutes": a positive integer estimating how long the topic takes to study
                                        - "estimatedHours": a number (e.g. 3.5) representing estimated hours

                                Return ONLY a valid JSON object with a single key "modules", like:
                                        {
                                                {
                                                        "modules": [
                                                        {
                                                        "title": "Java Fundamentals",
                                                        "description": "Learn Java syntax and core programming constructs to build a foundation for object-oriented programming.",
                                                        "topics": [
                                                                {
                                                                "name": "Variables and Data Types",
                                                                "description": "Understand primitive types, reference types, declarations, and type conversion.",
                                                                "estimatedMinutes": 60
                                                                },
                                                                {
                                                                "name": "Control Flow",
                                                                "description": "Use conditional statements and loops to control program execution.",
                                                                "estimatedMinutes": 90
                                                                },
                                                                {
                                                                "name": "Methods",
                                                                "description": "Organize reusable logic into methods with parameters and return values.",
                                                                "estimatedMinutes": 180
                                                                }
                                                        ],
                                                        "estimatedHours": 5.5
                                                        }
                                                        ]
                                                        }"""
                                .formatted(request.topic(), request.currentUnderstanding(), request.depthLevel(),
                                                request.goal());

        }

        @Transactional(readOnly = true)
        public SyllabusResponse getSyllabusById(Long id, User user) {
                Syllabus syllabus = syllabusRepository.findByIdAndUser(id, user)
                                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));

                return toResponseWithIds(syllabus);
        }

        @Transactional(readOnly = true)
        public List<SyllabusResponse> getMySyllabi(User user) {
                List<Syllabus> syllabi = syllabusRepository.findByUserOrderByCreatedAtDesc(user);

                return syllabi.stream()
                                .map(this::toResponseWithIds)
                                .toList();
        }

        @Transactional(readOnly = true)
        public DashboardResponse getDashboardSummary(User user) {
                List<Syllabus> syllabi = new ArrayList<>(
                                syllabusRepository.findByUserOrderByCreatedAtDesc(user));

                Comparator<LocalDateTime> newestFirst = Comparator.nullsLast(Comparator.reverseOrder());

                syllabi.sort(Comparator.comparing(this::lastActiveAt, newestFirst));

                int totalModules = 0;
                int completedModules = 0;
                int totalTopics = 0;
                int completedTopics = 0;

                List<DashboardResponse.LearningPathSummary> recentPaths = new ArrayList<>();

                DashboardResponse.ContinueLearning continueLearning = null;

                List<DashboardResponse.RecentActivity> activities = new ArrayList<>();

                for (Syllabus syllabus : syllabi) {
                        SyllabusResponse response = toResponseWithIds(syllabus);

                        int pathTotalModules = response.modules().size();

                        int pathCompletedModules = (int) response.modules().stream()
                                        .filter(ModuleDto::completed)
                                        .count();

                        int pathTotalTopics = response.modules().stream()
                                        .mapToInt(module -> module.topics().size())
                                        .sum();

                        int pathCompletedTopics = (int) response.modules().stream()
                                        .flatMap(module -> module.topics().stream())
                                        .filter(TopicDto::completed)
                                        .count();

                        double pathProgress = pathTotalTopics == 0
                                        ? 0.0
                                        : pathCompletedTopics * 100.0 / pathTotalTopics;

                        pathProgress = Math.round(pathProgress * 100.0) / 100.0;

                        totalModules += pathTotalModules;
                        completedModules += pathCompletedModules;
                        totalTopics += pathTotalTopics;
                        completedTopics += pathCompletedTopics;

                        String status;
                        if (pathTotalTopics > 0
                                        && pathCompletedTopics == pathTotalTopics
                                        && pathTotalModules > 0
                                        && pathCompletedModules == pathTotalModules) {
                                status = "COMPLETED";
                        } else if (pathCompletedTopics == 0) {
                                status = "NOT_STARTED";
                        } else {
                                status = "IN_PROGRESS";
                        }

                        recentPaths.add(new DashboardResponse.LearningPathSummary(
                                        syllabus.getId(),
                                        syllabus.getTopic(),
                                        isBlank(syllabus.getDescription())
                                                        ? syllabus.getTopic()
                                                        : syllabus.getDescription(),
                                        response.totalEstimatedHours(),
                                        pathTotalModules,
                                        pathCompletedModules,
                                        pathTotalTopics,
                                        pathCompletedTopics,
                                        pathProgress,
                                        status,
                                        lastActiveAt(syllabus)));

                        if (continueLearning == null && response.nextTopic() != null) {
                                SyllabusResponse.NextTopic next = response.nextTopic();

                                continueLearning = new DashboardResponse.ContinueLearning(
                                                syllabus.getId(),
                                                syllabus.getTopic(),
                                                next.moduleId(),
                                                next.moduleTitle(),
                                                next.topicId(),
                                                next.topicName());
                        }

                        if (syllabus.getModules() == null) {
                                continue;
                        }

                        for (SyllabusModule module : syllabus.getModules()) {
                                if (module == null || module.getTopics() == null) {
                                        continue;
                                }

                                for (Topic topic : module.getTopics()) {
                                        if (topic == null
                                                        || !topic.isCompleted()
                                                        || topic.getCompletedAt() == null) {
                                                continue;
                                        }

                                        activities.add(new DashboardResponse.RecentActivity(
                                                        "TOPIC_COMPLETED",
                                                        syllabus.getId(),
                                                        syllabus.getTopic(),
                                                        module.getId(),
                                                        module.getTitle(),
                                                        topic.getId(),
                                                        topic.getName(),
                                                        topic.getCompletedAt(),
                                                        "COMPLETED",
                                                        null,
                                                        null));
                                }
                        }
                }

                List<QuizAttempt> attempts = quizAttemptRepository
                                .findByUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(
                                                user.getId());

                int passedAttempts = (int) attempts.stream()
                                .filter(QuizAttempt::isPassed)
                                .count();

                double averageQuizScorePercent = attempts.stream()
                                .filter(attempt -> attempt.getTotalQuestions() > 0)
                                .mapToDouble(attempt -> attempt.getScore() * 100.0 / attempt.getTotalQuestions())
                                .average()
                                .orElse(0.0);

                averageQuizScorePercent = Math.round(averageQuizScorePercent * 100.0) / 100.0;

                for (QuizAttempt attempt : attempts) {
                        if (attempt.getQuiz() == null
                                        || attempt.getQuiz().getModule() == null
                                        || attempt.getQuiz().getModule().getSyllabus() == null) {
                                continue;
                        }

                        Quiz quiz = attempt.getQuiz();
                        SyllabusModule module = quiz.getModule();
                        Syllabus syllabus = module.getSyllabus();

                        // The repository query is user-scoped. This additional check
                        // prevents activity for paths outside this user's dashboard.
                        boolean belongsToUser = syllabi.stream()
                                        .anyMatch(path -> Objects.equals(
                                                        path.getId(), syllabus.getId()));

                        if (!belongsToUser) {
                                continue;
                        }

                        LocalDateTime occurredAt = attempt.getSubmittedAt() != null
                                        ? attempt.getSubmittedAt()
                                        : attempt.getAttemptedAt();

                        if (occurredAt == null) {
                                continue;
                        }

                        activities.add(new DashboardResponse.RecentActivity(
                                        "QUIZ_SUBMITTED",
                                        syllabus.getId(),
                                        syllabus.getTopic(),
                                        module.getId(),
                                        module.getTitle(),
                                        quiz.getId(),
                                        quiz.getTitle(),
                                        occurredAt,
                                        attempt.isPassed() ? "PASSED" : "FAILED",
                                        attempt.getScore(),
                                        attempt.getTotalQuestions()));
                }

                activities.sort(Comparator.comparing(
                                DashboardResponse.RecentActivity::occurredAt,
                                newestFirst));

                double overallProgress = totalTopics == 0
                                ? 0.0
                                : completedTopics * 100.0 / totalTopics;

                overallProgress = Math.round(overallProgress * 100.0) / 100.0;

                return new DashboardResponse(
                                syllabi.size(),
                                totalModules,
                                completedModules,
                                totalTopics,
                                completedTopics,
                                overallProgress,
                                attempts.size(),
                                passedAttempts,
                                averageQuizScorePercent,
                                continueLearning,
                                recentPaths.stream().limit(5).toList(),
                                activities.stream().limit(10).toList());
        }

        private LocalDateTime lastActiveAt(Syllabus syllabus) {
                if (syllabus.getUpdatedAt() != null) {
                        return syllabus.getUpdatedAt();
                }
                return syllabus.getCreatedAt();
        }

        private String extractJson(String raw) {
                String trimmed = raw.trim();
                if (trimmed.startsWith("```")) {
                        String[] lines = trimmed.split("\n");
                        if (lines.length >= 3) {
                                StringBuilder sb = new StringBuilder();
                                for (int i = 1; i < lines.length - 1; i++) {
                                        sb.append(lines[i]);
                                        if (i < lines.length - 1)
                                                sb.append("\n");
                                }
                                return sb.toString().trim();
                        }
                }
                return trimmed;
        }

        @Transactional(readOnly = true)
        public SyllabusResponse toResponseWithIds(Syllabus syllabus) {
                List<SyllabusModule> modules = syllabus.getModules() == null
                                ? List.of()
                                : syllabus.getModules();

                List<ModuleDto> moduleDtos = new ArrayList<>();

                Long userId = syllabus.getUser() != null
                                ? syllabus.getUser().getId()
                                : null;

                boolean previousModuleCompleted = true;
                SyllabusResponse.NextTopic nextTopic = null;

                int totalTopics = 0;
                int completedTopics = 0;

                for (SyllabusModule module : modules) {
                        List<Topic> topics = module.getTopics() == null
                                        ? List.of()
                                        : module.getTopics();

                        boolean moduleCompleted = false;

                        if (userId != null && module.getId() != null) {
                                moduleCompleted = quizRepository.findByModuleId(module.getId())
                                                .map(Quiz::getId)
                                                .map(quizId -> quizAttemptRepository
                                                                .existsByQuizIdAndUserIdAndPassedTrue(quizId, userId))
                                                .orElse(false);
                        }

                        boolean unlocked = previousModuleCompleted;

                        List<TopicDto> topicDtos = topics.stream()
                                        .map(topic -> new TopicDto(
                                                        topic.getId(),
                                                        topic.getName(),
                                                        isBlank(topic.getDescription())
                                                                        ? topic.getName()
                                                                        : topic.getDescription(),
                                                        topic.getEstimatedMinutes() == null
                                                                        || topic.getEstimatedMinutes() <= 0
                                                                                        ? 20
                                                                                        : topic.getEstimatedMinutes(),
                                                        topic.isCompleted()))
                                        .toList();

                        int moduleCompletedTopics = (int) topicDtos.stream()
                                        .filter(TopicDto::completed)
                                        .count();

                        totalTopics += topicDtos.size();
                        completedTopics += moduleCompletedTopics;

                        if (nextTopic == null && unlocked) {
                                for (Topic topic : topics) {
                                        if (!topic.isCompleted()) {
                                                nextTopic = new SyllabusResponse.NextTopic(
                                                                topic.getId(),
                                                                topic.getName(),
                                                                module.getId(),
                                                                module.getTitle());
                                                break;
                                        }
                                }
                        }

                        moduleDtos.add(new ModuleDto(
                                        module.getId(),
                                        module.getTitle(),
                                        isBlank(module.getDescription())
                                                        ? module.getTitle()
                                                        : module.getDescription(),
                                        module.getEstimatedHours(),
                                        moduleCompleted,
                                        unlocked,
                                        moduleCompletedTopics,
                                        topicDtos));

                        previousModuleCompleted = moduleCompleted;
                }

                double totalHours = modules.stream()
                                .mapToDouble(SyllabusModule::getEstimatedHours)
                                .sum();

                double progressPercent = totalTopics == 0
                                ? 0.0
                                : Math.round(completedTopics * 10000.0 / totalTopics) / 100.0;

                return new SyllabusResponse(
                                syllabus.getId(),
                                syllabus.getTopic(),
                                isBlank(syllabus.getDescription())
                                                ? syllabus.getTopic()
                                                : syllabus.getDescription(),
                                syllabus.getCurrentUnderstanding() == null
                                                ? null
                                                : syllabus.getCurrentUnderstanding().name(),
                                syllabus.getGoal(),
                                Math.round(totalHours * 100.0) / 100.0,
                                progressPercent,
                                nextTopic,
                                moduleDtos);
        }

        private boolean isBlank(String value) {
                return value == null || value.isBlank();
        }
}
