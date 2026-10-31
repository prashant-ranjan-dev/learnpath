package com.learnpath.version1.dto;

import java.util.List;

public record SyllabusResponse(
        Long syllabusId,
        String topic,
        String description,
        String currentUnderstanding,
        String goal,
        double totalEstimatedHours,
        double progressPercent,
        NextTopic nextTopic,
        List<ModuleDto> modules
) {
    public record NextTopic(
            Long topicId,
            String topicName,
            Long moduleId,
            String moduleTitle
    ) {}

    // Compatibility constructor for existing callers.
    public SyllabusResponse(
            Long syllabusId,
            String topic,
            List<ModuleDto> modules
    ) {
        this(
                syllabusId,
                topic,
                topic,
                null,
                null,
                modules.stream().mapToDouble(ModuleDto::estimatedHours).sum(),
                calculateProgress(modules),
                null,
                modules
        );
    }

    private static double calculateProgress(List<ModuleDto> modules) {
        int total = modules.stream().mapToInt(m -> m.topics().size()).sum();
        int completed = modules.stream()
                .flatMap(m -> m.topics().stream())
                .mapToInt(t -> t.completed() ? 1 : 0)
                .sum();

        return total == 0 ? 0.0 : Math.round(completed * 10000.0 / total) / 100.0;
    }
}