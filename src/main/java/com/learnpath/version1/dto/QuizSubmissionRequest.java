package com.learnpath.version1.dto;

import java.util.List;

public record QuizSubmissionRequest(List<Answer> answers) {

    public record Answer(
            Long questionId,
            Integer selectedIndex) {
    }
}
