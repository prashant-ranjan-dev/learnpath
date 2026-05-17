package com.learnpath.version1.dto;

import jakarta.validation.constraints.NotBlank;

public record SyllabusRequest(
        @NotBlank String topic,
        @NotBlank String currentUnderstanding,
        @NotBlank String depthLevel,
        @NotBlank String goal) {
}
