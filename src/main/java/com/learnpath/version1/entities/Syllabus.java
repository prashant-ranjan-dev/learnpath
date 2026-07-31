package com.learnpath.version1.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
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
    public static enum CurrentUnderstanding{
        BEGINNER, INTERMEDIATE, EXPERT
    }
    public static enum Depth{
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

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "syllabus", cascade = CascadeType.ALL)
    private List<SyllabusModule> modules;
}



