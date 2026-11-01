package com.learnpath.version1.dto;

import java.time.LocalDateTime;
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
        double averageQuizScorePercent,
        ContinueLearning continueLearning,
        List<LearningPathSummary> recentLearningPaths,
        List<RecentActivity> recentActivity) {

    public record ContinueLearning(
            Long syllabusId,
            String topic,
            Long moduleId,
            String moduleTitle,
            Long topicId,
            String topicTitle) {
    }

    public record LearningPathSummary(
            Long syllabusId,
            String topic,
            String description,
            double estimatedHours,
            int totalModules,
            int completedModules,
            int totalTopics,
            int completedTopics,
            double progressPercent,
            String status,
            LocalDateTime lastActiveAt) {
    }

    public record RecentActivity(
            String type,
            Long syllabusId,
            String learningPathTopic,
            Long moduleId,
            String moduleTitle,
            Long itemId,
            String title,
            LocalDateTime occurredAt,
            String status,
            Integer score,
            Integer totalQuestions) {
    }
}
