package com.example.ragApp.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationHistoryItemResponse(
        UUID conversationId,
        String summary,
        String category,
        String usecase,
        LocalDateTime createdAt,
        LocalDateTime lastMessageAt,
        long messageCount
) {}

