package com.learnpath.learnpath_backend.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LearningPathRequest {
    private Long userId;
    private String topic;
    private List<ModuleRequest> modules;

    @Getter
    @Setter
    private static class ModuleRequest{
        private String title;
        private List<String> objectives;
        private Integer estimitatedHours;
    }
}
