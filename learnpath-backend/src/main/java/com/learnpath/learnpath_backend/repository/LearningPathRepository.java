package com.learnpath.learnpath_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.learnpath.learnpath_backend.model.LearningPath;

public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {
    List<LearningPath> findUserById (Long userId);
}
