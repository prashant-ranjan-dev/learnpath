package com.learnpath.version1.controller;

import com.learnpath.version1.dto.*;
import com.learnpath.version1.utility.UserContext;
import com.learnpath.version1.repositories.QuizRepository;
import com.learnpath.version1.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService, QuizRepository quizRepository, UserContext userContext) {
        this.quizService = quizService;
    }

    // 1. Generate quiz for a module
    @PostMapping("/modules/{moduleId}")
    public ResponseEntity<QuizResponse> generateQuiz(@PathVariable Long moduleId) {
        QuizResponse response = quizService.generateQuiz(moduleId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<QuizResponse> getQuiz(@PathVariable Long quizId) {
        return ResponseEntity.ok(quizService.getQuiz(quizId));
    }

    // 2. Submit answers for a quiz
    @PostMapping("/{quizId}/submit")
    public ResponseEntity<QuizResultResponse> submitQuiz(@PathVariable Long quizId,
            @RequestBody QuizSubmissionRequest submission) {
        QuizResultResponse result = quizService.submitQuiz(quizId, submission);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{quizId}/analyze")
    public ResponseEntity<WeaknessAnalysisResponse> analyzeWeaknesses(@PathVariable Long quizId) {
        WeaknessAnalysisResponse response = quizService.analyzeWeaknesses(quizId);
        return ResponseEntity.ok(response);
    }

}