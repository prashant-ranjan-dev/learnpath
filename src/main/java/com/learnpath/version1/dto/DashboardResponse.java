package com.learnpath.version1.dto;

import java.util.List;

public record DashboardResponse(
        int totalLearningPaths,
        int totalModules,
        int completedModules,
        int totalTopics,
        int completedTopics,
        double progressPercent,
        int totalQuizAttempts,
        int passedQuizAttempts,
        List<LearningPathSummary> recentLearningPaths) {

    public record LearningPathSummary(
            Long syllabusId,
            String topic,
            int totalModules,
            int completedModules,
            int totalTopics,
            int completedTopics,
            double progressPercent) {
    }
}
