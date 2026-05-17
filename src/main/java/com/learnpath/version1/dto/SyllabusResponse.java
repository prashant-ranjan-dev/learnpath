package com.learnpath.version1.dto;

import java.util.List;

public record SyllabusResponse(
        Long syllabusId,
        List<ModuleDto> modules) {
}
