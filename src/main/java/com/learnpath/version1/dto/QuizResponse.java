package com.learnpath.version1.dto;

import java.util.List;

public record QuizResponse (Long quizId, List<QuestionDto> questions){
}
