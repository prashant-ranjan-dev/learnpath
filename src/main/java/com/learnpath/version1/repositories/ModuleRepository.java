package com.learnpath.version1.repositories;

import com.learnpath.version1.entities.SyllabusModule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleRepository extends JpaRepository<SyllabusModule, Long> {

}
