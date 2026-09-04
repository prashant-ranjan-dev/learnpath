package com.learnpath.version1.controller;

import com.learnpath.version1.dto.*;
import com.learnpath.version1.entities.Quiz;
import com.learnpath.version1.exception.ResourceNotFoundException;
import com.learnpath.version1.repositories.QuizRepository;
import com.learnpath.version1.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;
    private final QuizRepository quizRepository;

    public QuizController(QuizService quizService, QuizRepository quizRepository) {
        this.quizService = quizService;
        this.quizRepository = quizRepository;
    }

    // 1. Generate quiz for a module
    @PostMapping("/modules/{moduleId}")
    public ResponseEntity<QuizResponse> generateQuiz(@PathVariable Long moduleId) {
        QuizResponse response = quizService.generateQuiz(moduleId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<QuizResponse> getQuiz(@PathVariable Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        List<QuestionDto> questionDtos = quiz.getQuestions().stream()
                .map(q -> new QuestionDto(q.getId(), q.getQuestionText(),
                        q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()))
                .toList();
        return ResponseEntity.ok(new QuizResponse(quiz.getId(), questionDtos));
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