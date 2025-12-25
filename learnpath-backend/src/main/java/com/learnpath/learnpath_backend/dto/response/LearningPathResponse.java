package com.learnpath.learnpath_backend.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LearningPathResponse {
    private Long Id;
    private String topic;
    private Integer totalModules;
    private String status;
    private List<ModuleResponse> modules;
    
    @Getter
    @Builder
    private static class ModuleResponse{
        private Integer moduleNumber;
        private String title;
        private boolean isUnlocked;
        private boolean isCompleted;
    }
}
