package com.learnpath.version1.dto;

public record ContentResponse(
        Long topicId,
        String topicName,
        String content
) {}
