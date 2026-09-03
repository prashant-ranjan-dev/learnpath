package com.learnpath.version1.controller;

import com.learnpath.version1.dto.QuizResponse;
import com.learnpath.version1.dto.QuizResultResponse;
import com.learnpath.version1.dto.QuizSubmissionRequest;
import com.learnpath.version1.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    // 1. Generate quiz for a module
    @PostMapping("/modules/{moduleId}")
    public ResponseEntity<QuizResponse> generateQuiz(@PathVariable Long moduleId) {
        QuizResponse response = quizService.generateQuiz(moduleId);
        return ResponseEntity.ok(response);
    }

    // 2. Submit answers for a quiz
    @PostMapping("/{quizId}/submit")
    public ResponseEntity<QuizResultResponse> submitQuiz(@PathVariable Long quizId,
                                                         @RequestBody QuizSubmissionRequest submission) {
        QuizResultResponse result = quizService.submitQuiz(quizId, submission);
        return ResponseEntity.ok(result);
    }
}