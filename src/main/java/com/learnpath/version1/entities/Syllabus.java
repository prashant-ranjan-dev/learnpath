package com.learnpath.version1.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name= "syllabus")
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Syllabus {
    static enum CurrentUnderstanding{
        BEGINNER, INTERMEDIATE, EXPERT
    }
    static enum Depth{
        LEVEL1, LEVEL2, LEVEL3, LEVEL4, LEVEL5
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String topic;

    private CurrentUnderstanding currentUnderstanding ;

    private Depth depthLevel;

    private String goal;

    @OneToMany(mappedBy = "syllabus", cascade = CascadeType.ALL)
    private List<SyllabusModule> modules;
}



