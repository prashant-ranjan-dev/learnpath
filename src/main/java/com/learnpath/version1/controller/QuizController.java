
package com.learnpath.version1.controller;

import com.learnpath.version1.dto.QuizAttemptStartResponse;
import com.learnpath.version1.dto.QuizResponse;
import com.learnpath.version1.dto.QuizResultResponse;
import com.learnpath.version1.dto.QuizSubmissionRequest;
import com.learnpath.version1.dto.WeaknessAnalysisResponse;
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

    @PostMapping("/modules/{moduleId}")
    public ResponseEntity<QuizResponse> generateQuiz(
            @PathVariable Long moduleId
    ) {
        return ResponseEntity.ok(quizService.generateQuiz(moduleId));
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<QuizResponse> getQuiz(
            @PathVariable Long quizId
    ) {
        return ResponseEntity.ok(quizService.getQuiz(quizId));
    }

    @PostMapping("/{quizId}/attempts/start")
    public ResponseEntity<QuizAttemptStartResponse> startAttempt(
            @PathVariable Long quizId
    ) {
        return ResponseEntity.ok(quizService.startAttempt(quizId));
    }

    @PostMapping("/{quizId}/submit")
    public ResponseEntity<QuizResultResponse> submitQuiz(
            @PathVariable Long quizId,
            @RequestBody QuizSubmissionRequest submission
    ) {
        return ResponseEntity.ok(quizService.submitQuiz(quizId, submission));
    }

    @GetMapping("/{quizId}/result")
    public ResponseEntity<QuizResultResponse> getQuizResult(
            @PathVariable Long quizId
    ) {
        return ResponseEntity.ok(quizService.getQuizResult(quizId));
    }

    @PostMapping("/{quizId}/analyze")
    public ResponseEntity<WeaknessAnalysisResponse> analyzeWeaknesses(
            @PathVariable Long quizId
    ) {
        return ResponseEntity.ok(quizService.analyzeWeaknesses(quizId));
    }
}
