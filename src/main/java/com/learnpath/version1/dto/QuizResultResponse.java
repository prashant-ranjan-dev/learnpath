package com.learnpath.version1.dto;

import java.util.List;

public record QuizResultResponse(
        int score,
        int totalQuestions,
        boolean passed,
        List<Integer> correctAnswers,
        List<QuestionReview> review,
        int passMarkPercent,
        Long timeTakenSeconds
) {

    public record QuestionReview(
            Long questionId,
            String questionText,
            List<String> options,
            int selectedAnswerIndex,
            int correctAnswerIndex,
            boolean correct,
            String explanation
    ) {}
}
