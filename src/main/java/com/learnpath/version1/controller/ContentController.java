package com.learnpath.version1.controller;

import com.learnpath.version1.dto.ContentResponse;
import com.learnpath.version1.entities.User;
import com.learnpath.version1.service.ContentService;
import com.learnpath.version1.utility.UserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/topics")
public class ContentController {

    private final ContentService contentService;
    private final UserContext userContext;

    public ContentController(
            ContentService contentService,
            UserContext userContext) {
        this.contentService = contentService;
        this.userContext = userContext;
    }

    @GetMapping("/{topicId}/content")
    public ResponseEntity<ContentResponse> getTopicContent(
            @PathVariable Long topicId) {

        User currentUser = userContext.getCurrentUser();

        ContentResponse response = contentService.getOrGenerateContentForUser(
                topicId,
                currentUser.getId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{topicId}/complete")
    public ResponseEntity<Void> markTopicCompleted(
            @PathVariable Long topicId) {
        User currentUser = userContext.getCurrentUser();

        contentService.markTopicCompleted(
                topicId,
                currentUser.getId());

        return ResponseEntity.noContent().build();
    }
}