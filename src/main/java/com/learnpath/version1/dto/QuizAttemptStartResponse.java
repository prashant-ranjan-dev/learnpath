package com.learnpath.version1.dto;

import java.time.LocalDateTime;

public record QuizAttemptStartResponse(
        Long attemptId,
        Long quizId,
        LocalDateTime startedAt
) {}
