package com.example.ragApp.dto;

import java.util.List;

public record ConversationHistoryPageResponse(
        List<ConversationHistoryItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {}

