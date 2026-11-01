
package com.learnpath.version1.service;

import com.learnpath.version1.dto.ContentResponse;
import com.learnpath.version1.entities.Quiz;
import com.learnpath.version1.entities.Syllabus;
import com.learnpath.version1.entities.SyllabusModule;
import com.learnpath.version1.entities.Topic;
import com.learnpath.version1.exception.ResourceNotFoundException;
import com.learnpath.version1.repositories.QuizAttemptRepository;
import com.learnpath.version1.repositories.QuizRepository;
import com.learnpath.version1.repositories.TopicRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContentService {

    private final TopicRepository topicRepository;
    private final ChatClient chatClient;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public ContentService(
            TopicRepository topicRepository,
            ChatClient.Builder chatClientBuilder,
            QuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository) {
        this.topicRepository = topicRepository;
        this.chatClient = chatClientBuilder.build();
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    /**
     * Validates that the topic has the relationships required by the
     * learning workflow and returns its parent syllabus.
     */
    private Syllabus validateTopicRelationships(Topic topic) {
        SyllabusModule module = topic.getSyllabusModule();

        if (module == null || module.getSyllabus() == null) {
            throw new IllegalStateException(
                    "This topic is not associated with a valid learning path.");
        }

        return module.getSyllabus();
    }

    private void verifyOwnership(Syllabus syllabus, Long userId) {
        if (userId == null
                || syllabus.getUser() == null
                || syllabus.getUser().getId() == null
                || !syllabus.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                    "You are not authorized to access this learning content.");
        }
    }

    private void verifyModuleUnlocked(SyllabusModule module, Long userId) {
        Syllabus syllabus = module.getSyllabus();

        if (syllabus == null) {
            throw new IllegalStateException(
                    "This module is not associated with a valid learning path.");
        }

        List<SyllabusModule> modules = syllabus.getModules();

        if (modules == null || modules.isEmpty() || module.getId() == null) {
            throw new IllegalStateException(
                    "The learning path has no valid module sequence.");
        }

        int currentIndex = -1;

        for (int i = 0; i < modules.size(); i++) {
            SyllabusModule candidate = modules.get(i);

            if (candidate != null
                    && candidate.getId() != null
                    && candidate.getId().equals(module.getId())) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            throw new IllegalStateException(
                    "Module does not belong to the learning path.");
        }

        // The first module is always unlocked.
        if (currentIndex == 0) {
            return;
        }

        SyllabusModule previousModule = modules.get(currentIndex - 1);

        if (previousModule == null || previousModule.getId() == null) {
            throw new IllegalStateException(
                    "The previous module is invalid.");
        }

        boolean previousModulePassed = quizRepository
                .findByModuleId(previousModule.getId())
                .map(Quiz::getId)
                .map(quizId -> quizAttemptRepository
                        .existsByQuizIdAndUserIdAndPassedTrue(quizId, userId))
                .orElse(false);

        if (!previousModulePassed) {
            throw new IllegalStateException(
                    "Complete the previous module and pass its quiz "
                            + "to unlock this module.");
        }
    }

    /**
     * This method is intentionally user-scoped. Content must not be
     * returned or generated without checking ownership and module access.
     */
    @Transactional
    public ContentResponse getOrGenerateContentForUser(
            Long topicId,
            Long userId) {

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Topic not found with id: " + topicId));

        Syllabus syllabus = validateTopicRelationships(topic);
        verifyOwnership(syllabus, userId);
        verifyModuleUnlocked(topic.getSyllabusModule(), userId);

        String content = topic.getContent();

        if (content == null || content.isBlank()) {
            content = generateContentFromAi(topic);

            if (content == null || content.isBlank()) {
                throw new IllegalStateException(
                        "Content generation returned an empty response. Please try again.");
            }

            topic.setContent(content);
            topicRepository.save(topic);
        }

        // Reading existing content and generating new content both count
        // as activity for dashboard last-active ordering.
        syllabus.setUpdatedAt(LocalDateTime.now());

        return new ContentResponse(
                topic.getId(),
                topic.getName(),
                content);
    }

    @Transactional
    public void markTopicCompleted(Long topicId, Long userId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Topic not found with id: " + topicId));

        Syllabus syllabus = validateTopicRelationships(topic);
        verifyOwnership(syllabus, userId);

        if (topic.getContent() == null || topic.getContent().isBlank()) {
            throw new IllegalStateException(
                    "View the topic content before marking it as completed.");
        }

        verifyModuleUnlocked(topic.getSyllabusModule(), userId);

        // Keep completion idempotent: preserve the original completion time.
        if (!topic.isCompleted()) {
            LocalDateTime now = LocalDateTime.now();

            topic.setCompleted(true);
            topic.setCompletedAt(now);
            syllabus.setUpdatedAt(now);

            topicRepository.save(topic);
        }
    }

    private String generateContentFromAi(Topic topic) {
        Syllabus syllabus = validateTopicRelationships(topic);

        String prompt = """
                You are an expert tutor. Explain the topic below to a learner
                with the following profile:
                - Overall Goal: %s
                - Current Understanding: %s
                - Desired Depth: %s

                Topic: %s

                Provide a detailed, clear explanation, with examples where
                appropriate. Use Markdown formatting for headings, lists,
                and code blocks when relevant. Do not include a preamble.
                """.formatted(
                syllabus.getGoal(),
                syllabus.getCurrentUnderstanding(),
                syllabus.getDepthLevel(),
                topic.getName());

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
