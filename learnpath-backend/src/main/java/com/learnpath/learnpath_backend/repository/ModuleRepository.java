package com.learnpath.learnpath_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.learnpath.learnpath_backend.model.Module;

public interface ModuleRepository extends JpaRepository<Module, Long>{

    List<Module> findByLearningPathIdOrderByModuleNumber (Long learningPathId);
}
