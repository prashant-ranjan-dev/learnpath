package com.learnpath.version1.repositories;

import com.learnpath.version1.entities.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    Optional<QuizAttempt> findByQuizAndUserId(Long quizId, Long userId);
    List<QuizAttempt> findByUserIdOrderByAttemptedAtDesc(Long userId);
    Optional<QuizAttempt> findFirstByQuizIdAndUserIdOrderByAttemptedAtDesc(Long quizId, Long userId);
}
