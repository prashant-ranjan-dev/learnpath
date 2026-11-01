
package com.learnpath.version1.repositories;

import com.learnpath.version1.entities.WeaknessAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WeaknessAnalysisRepository
        extends JpaRepository<WeaknessAnalysis, Long> {

    Optional<WeaknessAnalysis> findByQuizAttemptId(Long quizAttemptId);
}
