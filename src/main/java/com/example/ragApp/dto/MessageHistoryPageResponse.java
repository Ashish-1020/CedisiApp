package com.example.ragApp.dto;

import java.util.List;
import java.util.UUID;

public record MessageHistoryPageResponse(
        UUID conversationId,
        List<MessageHistoryItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {}

