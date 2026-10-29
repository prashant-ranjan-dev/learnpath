package com.learnpath.version1.repositories;

import com.learnpath.version1.entities.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByUserIdOrderByAttemptedAtDesc(Long userId);

    Optional<QuizAttempt> findFirstByQuizIdAndUserIdOrderByAttemptedAtDesc(
            Long quizId,
            Long userId
    );

    Optional<QuizAttempt> findFirstByQuizIdAndUserIdAndUserAnswersJsonIsNullOrderByStartedAtDesc(
            Long quizId,
            Long userId
    );

    Optional<QuizAttempt> findFirstByQuizIdAndUserIdAndUserAnswersJsonIsNotNullOrderByAttemptedAtDesc(
            Long quizId,
            Long userId
    );

    boolean existsByQuizIdAndUserIdAndPassedTrue(
            Long quizId,
            Long userId
    );
}
