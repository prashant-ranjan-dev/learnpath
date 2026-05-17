package com.learnpath.version1.controller;

import com.learnpath.version1.dto.SyllabusRequest;
import com.learnpath.version1.dto.SyllabusResponse;
import com.learnpath.version1.service.SyllabusService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/syllabus")
public class SyllabusController {
    private final SyllabusService syllabusService;

    public SyllabusController (SyllabusService syllabusService){
        this.syllabusService = syllabusService;
    }

    @PostMapping
    public ResponseEntity<SyllabusResponse> generateSyllabus(
            @Valid @RequestBody SyllabusRequest request) {
        SyllabusResponse response = syllabusService.generateSyllabus(request);
        return ResponseEntity.ok(response);
    }
}
