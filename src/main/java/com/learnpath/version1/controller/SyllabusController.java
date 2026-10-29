package com.learnpath.version1.controller;

import com.learnpath.version1.dto.SyllabusRequest;
import com.learnpath.version1.dto.SyllabusResponse;
import com.learnpath.version1.entities.User;
import com.learnpath.version1.repositories.SyllabusRepository;
import com.learnpath.version1.service.SyllabusService;
import com.learnpath.version1.utility.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/syllabus")
public class SyllabusController {

    private final SyllabusService syllabusService;
    private final UserContext userContext;

    public SyllabusController(SyllabusService syllabusService,
            SyllabusRepository syllabusRepository,
            UserContext userContext) {
        this.syllabusService = syllabusService;
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
        return ResponseEntity.ok(syllabusService.getMySyllabi(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SyllabusResponse> getSyllabusById(@PathVariable Long id) {
        User currentUser = userContext.getCurrentUser();
        SyllabusResponse response = syllabusService.getSyllabusById(id, currentUser);
        return ResponseEntity.ok(response);
    }
}