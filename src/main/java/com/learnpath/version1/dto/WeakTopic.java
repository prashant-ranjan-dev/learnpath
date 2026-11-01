package com.learnpath.version1.dto;

public record WeakTopic(
        String topicName,
        String reason,
        String suggestion,
        String severity,
        String example
) {
}