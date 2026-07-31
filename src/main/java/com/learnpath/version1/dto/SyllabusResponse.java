package com.learnpath.version1.dto;

import com.learnpath.version1.dto.ModuleDto;

import java.util.List;

public record SyllabusResponse(Long syllabusId, String topic, List<ModuleDto> modules) {}