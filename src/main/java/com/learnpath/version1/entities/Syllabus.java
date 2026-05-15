package com.learnpath.version1.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

enum CurrentUnderstanding{
    Beginner, Intermediate, Expert
}
enum Depth{
    Level1, Level2, Level3, Level4, Level5
}
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String topic;

    private CurrentUnderstanding currentUnderstanding ;

    private Depth depthLevel;

    @OneToMany(mappedBy = "syllabus", cascade = CascadeType.ALL)
    private List<Module> modules;
}



