package com.learnpath.version1.controller;

import com.learnpath.version1.dto.*;
import com.learnpath.version1.entities.User;
import com.learnpath.version1.utility.UserContext;
import com.learnpath.version1.entities.Quiz;
import com.learnpath.version1.exception.ResourceNotFoundException;
import com.learnpath.version1.repositories.QuizRepository;
import com.learnpath.version1.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;


import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final UserContext userContext;
    private final QuizService quizService;
    private final QuizRepository quizRepository;

    public QuizController(QuizService quizService, QuizRepository quizRepository, UserContext userContext ) {
        this.quizService = quizService;
        this.quizRepository = quizRepository;
        this.userContext = userContext;
    }

    // 1. Generate quiz for a module
    @PostMapping("/modules/{moduleId}")
    public ResponseEntity<QuizResponse> generateQuiz(@PathVariable Long moduleId) {
        QuizResponse response = quizService.generateQuiz(moduleId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<QuizResponse> getQuiz(@PathVariable Long quizId) {
        User currentUser = userContext.getCurrentUser();

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));

        Long ownerId = quiz.getModule()
                .getSyllabus()
                .getUser()
                .getId();

        if (!ownerId.equals(currentUser.getId())) {
            throw new AccessDeniedException(
                "You do not have permission to access this resource"
            );
        }
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