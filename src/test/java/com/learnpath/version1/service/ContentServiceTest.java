
package com.learnpath.version1.service;

import com.learnpath.version1.entities.Syllabus;
import com.learnpath.version1.entities.SyllabusModule;
import com.learnpath.version1.entities.Topic;
import com.learnpath.version1.entities.User;
import com.learnpath.version1.exception.ResourceNotFoundException;
import com.learnpath.version1.repositories.QuizAttemptRepository;
import com.learnpath.version1.repositories.QuizRepository;
import com.learnpath.version1.repositories.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentServiceTest {

    private TopicRepository topicRepository;
    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private QuizRepository quizRepository;
    private QuizAttemptRepository quizAttemptRepository;
    private ContentService contentService;

    private User owner;
    private Topic topic;

    @BeforeEach
    void setUp() {
        topicRepository = mock(TopicRepository.class);
        chatClientBuilder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        quizRepository = mock(QuizRepository.class);
        quizAttemptRepository = mock(QuizAttemptRepository.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);

        contentService = new ContentService(
                topicRepository,
                chatClientBuilder,
                quizRepository,
                quizAttemptRepository);

        owner = User.builder()
                .id(10L)
                .email("owner@example.com")
                .password("test-password")
                .name("Owner")
                .build();

        Syllabus syllabus = Syllabus.builder()
                .id(20L)
                .user(owner)
                .modules(new ArrayList<>())
                .build();

        SyllabusModule module = SyllabusModule.builder()
                .id(30L)
                .syllabus(syllabus)
                .topics(new ArrayList<>())
                .build();

        syllabus.getModules().add(module);

        topic = Topic.builder()
                .id(40L)
                .name("Java Basics")
                .content("Existing topic content")
                .completed(false)
                .syllabusModule(module)
                .build();

        module.getTopics().add(topic);

        when(topicRepository.findById(40L))
                .thenReturn(Optional.of(topic));
    }

    @Test
    void rejectsContentAccessByAnotherUser() {
        assertThrows(
                AccessDeniedException.class,
                () -> contentService.getOrGenerateContentForUser(40L, 999L));
    }

    @Test
    void rejectsTopicWithoutSyllabusRelationship() {
        topic.setSyllabusModule(null);

        assertThrows(
                IllegalStateException.class,
                () -> contentService.getOrGenerateContentForUser(40L, 10L));
    }

    @Test
    void rejectsTopicWhoseModuleHasNoSyllabus() {
        topic.getSyllabusModule().setSyllabus(null);

        assertThrows(
                IllegalStateException.class,
                () -> contentService.getOrGenerateContentForUser(40L, 10L));
    }

    @Test
    void rejectsContentForModuleMissingFromSyllabusSequence() {
        topic.getSyllabusModule().getSyllabus().setModules(List.of());

        assertThrows(
                IllegalStateException.class,
                () -> contentService.getOrGenerateContentForUser(40L, 10L));
    }

    @Test
    void rejectsCompletionWhenTopicHasNoContent() {
        topic.setContent(" ");

        assertThrows(
                IllegalStateException.class,
                () -> contentService.markTopicCompleted(40L, 10L));
    }

    @Test
    void rejectsMissingTopic() {
        when(topicRepository.findById(404L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> contentService.getOrGenerateContentForUser(404L, 10L));
    }
}
