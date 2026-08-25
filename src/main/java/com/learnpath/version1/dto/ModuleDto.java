package com.learnpath.version1.dto;


import java.util.List;

public record ModuleDto(
        Long id,
        String title,
        double estimatedHours,
        List<TopicDto> topics
) {
}
