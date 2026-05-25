package com.learnpath.version1.repositories;

import com.learnpath.version1.entities.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {

}
