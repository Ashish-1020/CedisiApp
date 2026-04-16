package com.example.ragApp.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageHistoryItemResponse(
        UUID messageId,
        UUID conversationId,
        String role,
        String content,
        String category,
        String usecase,
        LocalDateTime timestamp
) {}

