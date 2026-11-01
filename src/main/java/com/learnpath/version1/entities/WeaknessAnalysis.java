
package com.learnpath.version1.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "weakness_analyses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_weakness_analysis_attempt",
                columnNames = "quiz_attempt_id"
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WeaknessAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "quiz_attempt_id",
            nullable = false,
            unique = true
    )
    private QuizAttempt quizAttempt;

    /**
     * JSON-serialized list of WeakTopic entries.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String weakTopicsJson;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt;
}
