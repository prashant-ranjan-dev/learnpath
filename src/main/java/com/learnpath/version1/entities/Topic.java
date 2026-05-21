package com.learnpath.version1.entities;

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

    @Column(nullable = true, columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JoinColumn(name = "syllabus_module_id")
    private SyllabusModule syllabusModule;
}