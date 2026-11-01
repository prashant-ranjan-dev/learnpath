package com.learnpath.version1.entities;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Integer estimatedMinutes = 20;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean completed = false;

    @Column 
    private LocalDateTime completedAt;

    @ManyToOne
    @JoinColumn(name = "syllabus_module_id")
    private SyllabusModule syllabusModule;
}