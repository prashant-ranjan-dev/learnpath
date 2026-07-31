package com.learnpath.version1.controller;

import com.learnpath.version1.dto.SyllabusRequest;
import com.learnpath.version1.dto.SyllabusResponse;
import com.learnpath.version1.entities.Syllabus;
import com.learnpath.version1.entities.User;
import com.learnpath.version1.exception.ResourceNotFoundException;
import com.learnpath.version1.repositories.SyllabusRepository;
import com.learnpath.version1.service.SyllabusService;
import com.learnpath.version1.utility.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/syllabus")
public class SyllabusController {

    private final SyllabusService syllabusService;
    private final SyllabusRepository syllabusRepository;
    private final UserContext userContext;

    public SyllabusController(SyllabusService syllabusService,
                              SyllabusRepository syllabusRepository,
                              UserContext userContext) {
        this.syllabusService = syllabusService;
        this.syllabusRepository = syllabusRepository;
        this.userContext = userContext;
    }

    @PostMapping
    public ResponseEntity<SyllabusResponse> generateSyllabus(
            @Valid @RequestBody SyllabusRequest request) {
        SyllabusResponse response = syllabusService.generateSyllabus(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<SyllabusResponse>> getMySyllabi() {
        User currentUser = userContext.getCurrentUser();
        List<Syllabus> syllabi = syllabusRepository.findByUserOrderByCreatedAtDesc(currentUser);
        List<SyllabusResponse> responses = syllabi.stream()
                .map(syllabusService::toResponseWithIds)   // now accessible
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SyllabusResponse> getSyllabusById(@PathVariable Long id) {
        User currentUser = userContext.getCurrentUser();
        Syllabus syllabus = syllabusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));
        if (!syllabus.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(syllabusService.toResponseWithIds(syllabus));
    }
}