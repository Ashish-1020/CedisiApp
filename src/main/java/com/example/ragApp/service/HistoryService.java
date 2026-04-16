package com.example.ragApp.service;

import com.example.ragApp.dto.ConversationDetailsResponse;
import com.example.ragApp.dto.ConversationHistoryPageResponse;
import com.example.ragApp.dto.MessageHistoryPageResponse;

import java.util.UUID;

public interface HistoryService {

    ConversationHistoryPageResponse getConversations(String userId, int page, int size);

    ConversationDetailsResponse getConversation(UUID conversationId, String userId);

    MessageHistoryPageResponse getConversationMessages(UUID conversationId, String userId, int page, int size, String sort);
}

