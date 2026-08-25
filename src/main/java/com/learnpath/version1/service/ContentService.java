package com.learnpath.version1.service;

import com.learnpath.version1.dto.ContentResponse;
import com.learnpath.version1.entities.Syllabus;
import com.learnpath.version1.entities.SyllabusModule;
import com.learnpath.version1.entities.Topic;
import com.learnpath.version1.exception.ResourceNotFoundException;
import com.learnpath.version1.repositories.TopicRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService {
    private final TopicRepository  topicRepository;
    private final ChatClient chatClient;

    public ContentService (TopicRepository topicRepository, ChatClient.Builder chatBuilderBuilder){
        this.topicRepository = topicRepository;
        this.chatClient = chatBuilderBuilder.build();
    }

    @Transactional
    public ContentResponse getOrGenerateContent(Long topicId){
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " +topicId));

        if (topic.getContent() != null && !topic.getContent().isBlank()){
            return new ContentResponse(topic.getId(), topic.getName(), topic.getContent());
        }

        String generatedContent = generateContentFromAi(topic);

        topic.setContent(generatedContent);
        topicRepository.save(topic);

        return new ContentResponse(topic.getId(), topic.getName(), generatedContent);
    }

    private String generateContentFromAi(Topic topic){
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
