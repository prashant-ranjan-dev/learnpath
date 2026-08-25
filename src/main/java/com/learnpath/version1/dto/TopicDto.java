package com.learnpath.version1.dto;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public record TopicDto(
        Long id,
        String name
) {
}
