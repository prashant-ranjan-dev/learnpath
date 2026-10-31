
package com.learnpath.version1.dto;

import java.time.LocalDateTime;

public record QuizAttemptHistoryResponse(
        Long attemptId,
        Long quizId,
        int score,
        int totalQuestions,
        boolean passed,
        LocalDateTime attemptedAt,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        Long timeTakenSeconds) {
}
