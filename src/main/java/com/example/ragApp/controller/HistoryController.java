package com.example.ragApp.controller;

import com.example.ragApp.dto.ConversationDetailsResponse;
import com.example.ragApp.dto.ConversationHistoryPageResponse;
import com.example.ragApp.dto.MessageHistoryPageResponse;
import com.example.ragApp.service.HistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<ConversationHistoryPageResponse> getConversations(
            @RequestParam("userId") String userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(historyService.getConversations(userId, page, size));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ConversationDetailsResponse> getConversation(
            @PathVariable UUID conversationId,
            @RequestParam("userId") String userId
    ) {
        return ResponseEntity.ok(historyService.getConversation(conversationId, userId));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessageHistoryPageResponse> getConversationMessages(
            @PathVariable UUID conversationId,
            @RequestParam("userId") String userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size,
            @RequestParam(value = "sort", defaultValue = "timestamp,asc") String sort
    ) {
        return ResponseEntity.ok(historyService.getConversationMessages(conversationId, userId, page, size, sort));
    }
}

