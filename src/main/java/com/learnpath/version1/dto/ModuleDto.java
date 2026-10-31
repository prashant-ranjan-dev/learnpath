package com.learnpath.version1.dto;

import java.util.List;

public record ModuleDto(
        Long id,
        String title,
        String description,
        double estimatedHours,
        boolean completed,
        boolean unlocked,
        int completedTopics,
        List<TopicDto> topics
) {
    public ModuleDto(
            Long id,
            String title,
            double estimatedHours,
            boolean completed,
            boolean unlocked,
            List<TopicDto> topics
    ) {
        this(
                id,
                title,
                title,
                estimatedHours,
                completed,
                unlocked,
                (int) topics.stream().filter(TopicDto::completed).count(),
                topics
        );
    }
}