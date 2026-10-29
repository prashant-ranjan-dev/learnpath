package com.learnpath.version1.dto;

import java.util.List;

public record QuizResponse(
        Long quizId,
        String title,
        String description,
        int passMarkPercent,
        List<QuestionDto> questions
) {}
