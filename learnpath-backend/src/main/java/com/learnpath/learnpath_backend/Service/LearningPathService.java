package com.learnpath.learnpath_backend.Service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.learnpath.learnpath_backend.model.LearningPath;
import com.learnpath.learnpath_backend.repository.LearningPathRepository;
import com.learnpath.learnpath_backend.repository.ModuleRepository;
import com.learnpath.learnpath_backend.repository.UserRepository;
import com.learnpath.learnpath_backend.model.User;
import com.learnpath.learnpath_backend.model.Module;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LearningPathService {
    private final LearningPathRepository learningPathRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;

    @Transactional
    public LearningPath createLearningPath(
        Long userId,
        String topic,
        List <Module> modules
    ){
        User user  = userRepository.findById(userId).orElseThrow(() -> new RuntimeException());

        LearningPath learningPath = LearningPath.builder()
                        .user(user)
                        .topic(topic)
                        .totalModules(modules.size())
                        .Status(LearningPath.PathStatus.ACTIVE)
                        .build();

        learningPath = learningPathRepository.save(learningPath);

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            module.setLearningPath(learningPath);
            module.setModuleNumber(i+1);
            module.setUnlocked(i == 0);
            moduleRepository.save(module);

        }

        return learningPath;

    }

    public List<LearningPath> getLearningPathForUser (Long userId){
        return learningPathRepository.findUserById(userId);
    }
}
