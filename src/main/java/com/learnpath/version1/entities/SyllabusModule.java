package com.learnpath.version1.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "syllabusModule")
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SyllabusModule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String title;

    private double estimatedHours;

    @ManyToOne
    @JoinColumn(name ="syllabus_id")
    private Syllabus syllabus;

    @OneToMany(mappedBy = "syllabusModule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Topic> topics;
}
