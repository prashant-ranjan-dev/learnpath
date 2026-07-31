package com.learnpath.version1.repositories;

import com.learnpath.version1.entities.Syllabus;
import com.learnpath.version1.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SyllabusRepository extends JpaRepository<Syllabus, Long> {
    List<Syllabus> findByUserOrderByCreatedAtDesc(User user);
    Optional<Syllabus> findByIdAndUser(Long id, User user);  // not strictly needed
}
