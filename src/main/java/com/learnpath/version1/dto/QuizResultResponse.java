package com.learnpath.version1.dto;

import java.util.List;

public record QuizResultResponse (int score, int totalQuestions, boolean passed, List<Integer> correctAnswers){
}
