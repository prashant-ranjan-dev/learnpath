package com.learnpath.version1.dto;

import java.time.LocalDateTime;
import java.util.List;

public record WeaknessAnalysisResponse(
        List<WeakTopic> weakTopics,
        LocalDateTime generatedAt
) {
}