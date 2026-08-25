package com.learnpath.version1.service;

import com.learnpath.version1.dto.ModuleDto;
import com.learnpath.version1.dto.SyllabusRequest;
import com.learnpath.version1.dto.SyllabusResponse;
import com.learnpath.version1.dto.TopicDto;
import com.learnpath.version1.entities.Syllabus;
import com.learnpath.version1.entities.SyllabusModule;
import com.learnpath.version1.entities.Topic;
import com.learnpath.version1.exception.AiResponseParsingException;
import com.learnpath.version1.repositories.SyllabusRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class SyllabusService {

    record AiSyllabusResponse(List<AiModule> modules) {}

    record AiModule(String title, double estimatedHours, List<String> topics) {}

    private final ChatClient chatClient;
    private final SyllabusRepository syllabusRepository;
    private final ObjectMapper objectMapper;

    public SyllabusService(ChatClient.Builder chatClientBuilder,
                           SyllabusRepository syllabusRepository,
                           ObjectMapper objectMapper){
        this.chatClient = chatClientBuilder.build();
        this.syllabusRepository = syllabusRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SyllabusResponse generateSyllabus(SyllabusRequest request) {
        // Build the prompt
        String prompt = buildPrompt(request);

        // Call the AI agent using the Google Gemini
        String aiOutput = chatClient.prompt()
                .system("You are an expert curriculum designer. You create detailed, structured syllabi for learners. \n" +
                        "Always respond in perfect JSON matching the user's requested schema. Do not include any extra text, markdown, or code fences.")
                .user(prompt)
                .call()
                .content();

        // Extract the Json from the AI Response
        String json = extractJson(aiOutput);
        AiSyllabusResponse aiResponse;
        try{
            aiResponse = objectMapper.readValue(json, AiSyllabusResponse.class);
        } catch (JacksonException e) {
            throw new AiResponseParsingException(
                    "Failed to parse AI syllabus JSON. Raw output: " + aiOutput, e);

        }

        Syllabus syllabus = Syllabus.builder()
                .topic(request.topic())
                .currentUnderstanding(Syllabus.CurrentUnderstanding.valueOf(request.currentUnderstanding()))
                .depthLevel(Syllabus.Depth.valueOf(request.depthLevel()))
                .goal(request.goal())
                .modules(new ArrayList<>())
                .build();

        List<SyllabusModule> moduleEntities = new ArrayList<>();

        for (AiModule aiMod : aiResponse.modules()){
            SyllabusModule mod = SyllabusModule.builder()
                    .title(aiMod.title())
                    .estimatedHours((float) aiMod.estimatedHours())
                    .syllabus(syllabus)
                    .topics(new ArrayList<>())
                    .build();

            for (String topicName : aiMod.topics()){
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

    private String buildPrompt(SyllabusRequest request){
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
                """.formatted(request.topic(), request.currentUnderstanding(), request.depthLevel(), request.goal());

    }

    private String extractJson (String raw){
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")){
            String[] lines = trimmed.split("\n");
            if (lines.length >= 3){
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < lines.length-1; i++) {
                    sb.append(lines[i]);
                    if (i < lines.length -1)sb.append("\n");
                }
                return sb.toString().trim();
            }
        }
        return trimmed;
    }


    private SyllabusResponse toResponseWithIds(Syllabus syllabus){
        List<ModuleDto> moduleDtos = syllabus.getModules().stream()
                .map(mod -> new ModuleDto(
                        mod.getId(),
                        mod.getTitle(),
                        mod.getEstimatedHours(),
                        mod.getTopics().stream()
                                .map(t -> new TopicDto(t.getId(), t.getName()))
                                .collect(toList())

                )).collect(toList());

        return new SyllabusResponse(syllabus.getId(), moduleDtos);
    }
}

