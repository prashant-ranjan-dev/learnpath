package com.learnpath.version1.controller;

import com.learnpath.version1.dto.ContentResponse;
import com.learnpath.version1.service.ContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topics")
public class ContentController {
    private final ContentService contentService;

    public ContentController(ContentService contentService){
        this.contentService = contentService;
    }

    @GetMapping("/{topicId}/content")
    public ResponseEntity<ContentResponse> getTopicContent(@PathVariable Long topicId){
        ContentResponse response = contentService.getOrGenerateContent(topicId);
        return ResponseEntity.ok(response);
    }
}
