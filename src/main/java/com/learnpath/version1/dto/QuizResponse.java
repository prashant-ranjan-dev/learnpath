package com.learnpath.version1.dto;

import java.util.List;

public record QuizResponse(
        Long quizId,
        String title,
        String description,
        int passMarkPercent,
        int durationMinutes,
        int questionCount,
        List<QuestionDto> questions
) {
    public QuizResponse(
            Long quizId,
            String title,
            String description,
            int passMarkPercent,
            List<QuestionDto> questions
    ) {
        this(
                quizId,
                title,
                description,
                passMarkPercent,
                15,
                questions == null ? 0 : questions.size(),
                questions
        );
    }
}