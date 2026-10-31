package com.learnpath.version1.dto;

public record TopicDto(
        Long id,
        String name,
        String description,
        int estimatedMinutes,
        boolean completed
) {
    // Retains compatibility with existing tests and callers.
    public TopicDto(Long id, String name, boolean completed) {
        this(id, name, name, 20, completed);
    }
}