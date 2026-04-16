package com.example.ragApp.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationDetailsResponse(
        UUID conversationId,
        String userId,
        String summary,
        String category,
        String usecase,
        LocalDateTime createdAt,
        long messageCount
) {}

