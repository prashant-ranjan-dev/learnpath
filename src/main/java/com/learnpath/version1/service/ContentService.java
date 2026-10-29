package com.learnpath.version1.service;

import com.learnpath.version1.dto.ContentResponse;
import com.learnpath.version1.entities.Syllabus;
import com.learnpath.version1.entities.SyllabusModule;
import com.learnpath.version1.entities.Topic;
import com.learnpath.version1.entities.Quiz;
import com.learnpath.version1.exception.ResourceNotFoundException;
import com.learnpath.version1.repositories.QuizRepository;
import com.learnpath.version1.repositories.QuizAttemptRepository;
import com.learnpath.version1.repositories.TopicRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;


@Service
public class ContentService {
    private final TopicRepository topicRepository;
    private final ChatClient chatClient;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public ContentService(TopicRepository topicRepository, ChatClient.Builder chatBuilderBuilder, QuizRepository quizRepository, QuizAttemptRepository quizAttemptRepository) {
        this.topicRepository = topicRepository;
        this.chatClient = chatBuilderBuilder.build();
        this.quizRepository = quizRepository;
this.quizAttemptRepository = quizAttemptRepository;
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

    // The first module is always unlocked.
    if (currentIndex == 0) {
        return;
    }

    SyllabusModule previousModule = modules.get(currentIndex - 1);

    boolean previousModulePassed = quizRepository
            .findByModuleId(previousModule.getId())
            .map(Quiz::getId)
            .map(quizId -> quizAttemptRepository
                    .existsByQuizIdAndUserIdAndPassedTrue(quizId, userId))
            .orElse(false);

    if (!previousModulePassed) {
        throw new IllegalStateException(
                "Complete the previous module and pass its quiz to unlock this module.");
    }
}

    @Transactional
    public ContentResponse getOrGenerateContent(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));

        if (topic.getContent() != null && !topic.getContent().isBlank()) {
            return new ContentResponse(topic.getId(), topic.getName(), topic.getContent());
        }

        String generatedContent = generateContentFromAi(topic);

        topic.setContent(generatedContent);
        topicRepository.save(topic);

        return new ContentResponse(topic.getId(), topic.getName(), generatedContent);
    }

    @Transactional
    public ContentResponse getOrGenerateContentForUser(
            Long topicId,
            Long userId) {

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Topic not found with id: " + topicId));

        SyllabusModule module = topic.getSyllabusModule();
        Syllabus syllabus = module.getSyllabus();

        if (syllabus.getUser() == null
                || !syllabus.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not authorized to access this topic");
        }
        
        verifyModuleUnlocked(module, userId);

        return getOrGenerateContent(topicId);
    }

    @Transactional
    public void markTopicCompleted(Long topicId, Long userId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Topic not found with id: " + topicId));

        SyllabusModule module = topic.getSyllabusModule();
        Syllabus syllabus = module.getSyllabus();

        if (syllabus.getUser() == null
                || !syllabus.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not authorized to complete this topic");
        }

        if (topic.getContent() == null || topic.getContent().isBlank()) {
            throw new IllegalStateException(
                    "View the topic content before marking it as completed.");
        }

        verifyModuleUnlocked(module, userId);

        if (!topic.isCompleted()) {
            topic.setCompleted(true);
            topicRepository.save(topic);
        }
    }

    private String generateContentFromAi(Topic topic) {
        SyllabusModule module = topic.getSyllabusModule();
        Syllabus syllabus = module.getSyllabus();

        String prompt = """
                You are an expert tutor. Explain the topic below to a learner with the following profile:
                                - Overall Goal: %s
                                - Current Understanding: %s
                                - Desired Depth: %s

                                Topic: %s

                                Provide a detailed, clear explanation, with examples where appropriate.
                                Use Markdown formatting for headings, lists, and code blocks (if relevant).
                                Do not include any preamble like "Sure, here is the explanation:".
                                """.formatted(syllabus.getGoal(),
                syllabus.getCurrentUnderstanding(),
                syllabus.getDepthLevel(),
                topic.getName());

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();

    }
}
