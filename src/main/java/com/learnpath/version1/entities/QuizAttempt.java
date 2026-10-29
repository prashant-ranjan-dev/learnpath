package com.learnpath.version1.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private int score;

    private int totalQuestions;

    private boolean passed;

    /**
     * Legacy timestamp retained for compatibility with existing queries
     * and previously stored attempts.
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime attemptedAt;

    /**
     * Explicit start time for the attempt lifecycle.
     */
    private LocalDateTime startedAt;

    /**
     * Null while an attempt is in progress.
     * Legacy completed attempts may also have null here.
     */
    private LocalDateTime submittedAt;

    /**
     * Duration in whole seconds, calculated when the attempt is submitted.
     */
    private Long timeTakenSeconds;

    /**
     * Canonically ordered selected-option indices, serialized as JSON.
     * A null value indicates an attempt has not yet been submitted.
     */
    @Column(columnDefinition = "TEXT")
    private String userAnswersJson;
}
