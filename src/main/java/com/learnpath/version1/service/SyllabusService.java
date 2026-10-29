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
import com.learnpath.version1.repositories.UserRepository;
import com.learnpath.version1.repositories.QuizAttemptRepository;
import com.learnpath.version1.repositories.QuizRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class SyllabusService {

        record AiSyllabusResponse(List<AiModule> modules) {
        }

        record AiModule(String title, double estimatedHours, List<String> topics) {
        }

        private final ChatClient chatClient;
        private final SyllabusRepository syllabusRepository;
        private final ObjectMapper objectMapper;
        private final UserRepository userRepository;
        private final QuizRepository quizRepository;
        private final QuizAttemptRepository quizAttemptRepository;

        public SyllabusService(ChatClient.Builder chatClientBuilder,
                        SyllabusRepository syllabusRepository,
                        ObjectMapper objectMapper,
                        UserRepository userRepository, QuizRepository quizRepository,
                        QuizAttemptRepository quizAttemptRepository) {
                this.chatClient = chatClientBuilder.build();
                this.syllabusRepository = syllabusRepository;
                this.objectMapper = objectMapper;
                this.userRepository = userRepository;
                this.quizRepository = quizRepository;
                this.quizAttemptRepository = quizAttemptRepository;
        }

        private User getCurrentUser() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String email = authentication.getName();
                return userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
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
                                        || module.title().isBlank()) {
                                throw new AiResponseParsingException(
                                                "Each AI-generated module must have a title.",
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

                        for (String topic : module.topics()) {
                                if (topic == null || topic.isBlank()) {
                                        throw new AiResponseParsingException(
                                                        "AI-generated topic names must not be blank.",
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
                                .user(getCurrentUser())
                                .modules(new ArrayList<>())
                                .build();

                List<SyllabusModule> moduleEntities = new ArrayList<>();

                for (AiModule aiMod : aiResponse.modules()) {
                        SyllabusModule mod = SyllabusModule.builder()
                                        .title(aiMod.title())
                                        .estimatedHours((float) aiMod.estimatedHours())
                                        .syllabus(syllabus)
                                        .topics(new ArrayList<>())
                                        .build();

                        for (String topicName : aiMod.topics()) {
                                Topic topic = Topic.builder()
                                                .name(topicName)
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
                                            - "topics": a list of 3-5 specific topics (strings)
                                            - "estimatedHours": a number (e.g. 3.5) representing estimated hours

                                            Return ONLY a valid JSON object with a single key "modules", like:
                                            {
                                              "modules": [
                                                {
                                                  "title": "Module Title",
                                                  "topics": ["Topic 1", "Topic 2", "Topic 3"],
                                                  "estimatedHours": 5.5
                                                }
                                              ]
                                            }
                                """.formatted(request.topic(), request.currentUnderstanding(), request.depthLevel(),
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
                List<Syllabus> syllabi = syllabusRepository.findByUserOrderByCreatedAtDesc(user);

                int totalModules = 0;
                int completedModules = 0;
                int totalTopics = 0;
                int completedTopics = 0;

                List<DashboardResponse.LearningPathSummary> recentPaths = new ArrayList<>();

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

                        totalModules += pathTotalModules;
                        completedModules += pathCompletedModules;
                        totalTopics += pathTotalTopics;
                        completedTopics += pathCompletedTopics;

                        recentPaths.add(new DashboardResponse.LearningPathSummary(
                                        syllabus.getId(),
                                        syllabus.getTopic(),
                                        pathTotalModules,
                                        pathCompletedModules,
                                        pathTotalTopics,
                                        pathCompletedTopics,
                                        Math.round(pathProgress * 100.0) / 100.0));
                }

                List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByAttemptedAtDesc(
                                user.getId());

                int passedAttempts = (int) attempts.stream()
                                .filter(QuizAttempt::isPassed)
                                .count();

                double overallProgress = totalTopics == 0
                                ? 0.0
                                : completedTopics * 100.0 / totalTopics;

                return new DashboardResponse(
                                syllabi.size(),
                                totalModules,
                                completedModules,
                                totalTopics,
                                completedTopics,
                                Math.round(overallProgress * 100.0) / 100.0,
                                attempts.size(),
                                passedAttempts,
                                recentPaths.stream().limit(5).toList());
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
                List<SyllabusModule> modules = syllabus.getModules();
                List<ModuleDto> moduleDtos = new ArrayList<>();

                Long userId = syllabus.getUser() != null
                                ? syllabus.getUser().getId()
                                : null;

                boolean previousModuleCompleted = true;

                for (SyllabusModule module : modules) {
                        boolean completed = false;

                        if (userId != null && module.getId() != null) {
                                completed = quizRepository.findByModuleId(module.getId())
                                                .map(Quiz::getId)
                                                .map(quizId -> quizAttemptRepository
                                                                .existsByQuizIdAndUserIdAndPassedTrue(
                                                                                quizId, userId))
                                                .orElse(false);
                        }

                        boolean unlocked = previousModuleCompleted;

                        List<TopicDto> topicDtos = module.getTopics().stream()
                                        .map(topic -> new TopicDto(
                                                        topic.getId(),
                                                        topic.getName(),
                                                        topic.isCompleted()))
                                        .toList();

                        moduleDtos.add(new ModuleDto(
                                        module.getId(),
                                        module.getTitle(),
                                        module.getEstimatedHours(),
                                        completed,
                                        unlocked,
                                        topicDtos));

                        previousModuleCompleted = completed;
                }

                return new SyllabusResponse(
                                syllabus.getId(),
                                syllabus.getTopic(),
                                moduleDtos);
        }
}
