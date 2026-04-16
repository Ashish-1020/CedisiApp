package com.example.ragApp.service;

import com.example.ragApp.data.Conversation;
import com.example.ragApp.data.Message;
import com.example.ragApp.dto.ConversationDetailsResponse;
import com.example.ragApp.dto.ConversationHistoryItemResponse;
import com.example.ragApp.dto.ConversationHistoryPageResponse;
import com.example.ragApp.dto.MessageHistoryItemResponse;
import com.example.ragApp.dto.MessageHistoryPageResponse;
import com.example.ragApp.repository.ConversationRepository;
import com.example.ragApp.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class HistoryServiceImpl implements HistoryService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public HistoryServiceImpl(ConversationRepository conversationRepository,
                              MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public ConversationHistoryPageResponse getConversations(String userId, int page, int size) {
        validateUserId(userId);

        Pageable pageable = PageRequest.of(safePage(page), safeSize(size));
        Page<Conversation> conversationsPage = conversationRepository
                .findByUserIdOrderByCreatedAtDesc(userId.trim(), pageable);

        List<ConversationHistoryItemResponse> items = conversationsPage.getContent()
                .stream()
                .map(this::toConversationHistoryItem)
                .toList();

        return new ConversationHistoryPageResponse(
                items,
                conversationsPage.getNumber(),
                conversationsPage.getSize(),
                conversationsPage.getTotalElements(),
                conversationsPage.getTotalPages(),
                conversationsPage.hasNext()
        );
    }

    @Override
    public ConversationDetailsResponse getConversation(UUID conversationId, String userId) {
        Conversation conversation = getOwnedConversation(conversationId, userId);
        long messageCount = messageRepository.countByConversationId(conversation.getConversationId());

        return new ConversationDetailsResponse(
                conversation.getConversationId(),
                conversation.getUserId(),
                conversation.getSummary(),
                conversation.getCategory(),
                conversation.getUsecase(),
                conversation.getCreatedAt(),
                messageCount
        );
    }

    @Override
    public MessageHistoryPageResponse getConversationMessages(UUID conversationId,
                                                              String userId,
                                                              int page,
                                                              int size,
                                                              String sort) {
        Conversation conversation = getOwnedConversation(conversationId, userId);

        Sort sortByTimestamp = parseSort(sort);
        Pageable pageable = PageRequest.of(safePage(page), safeSize(size), sortByTimestamp);

        Page<Message> messagesPage = messageRepository.findByConversationId(conversation.getConversationId(), pageable);

        List<MessageHistoryItemResponse> messages = messagesPage.getContent()
                .stream()
                .map(this::toMessageHistoryItem)
                .toList();

        return new MessageHistoryPageResponse(
                conversation.getConversationId(),
                messages,
                messagesPage.getNumber(),
                messagesPage.getSize(),
                messagesPage.getTotalElements(),
                messagesPage.getTotalPages(),
                messagesPage.hasNext()
        );
    }

    private Conversation getOwnedConversation(UUID conversationId, String userId) {
        validateUserId(userId);

        if (conversationId == null) {
            throw new RuntimeException("conversationId is required");
        }

        return conversationRepository
                .findByConversationIdAndUserId(conversationId, userId.trim())
                .orElseThrow(() -> new RuntimeException("Conversation not found for user"));
    }

    private ConversationHistoryItemResponse toConversationHistoryItem(Conversation conversation) {
        long messageCount = messageRepository.countByConversationId(conversation.getConversationId());
        var latestMessage = messageRepository.findTopByConversationIdOrderByTimestampDesc(conversation.getConversationId());

        return new ConversationHistoryItemResponse(
                conversation.getConversationId(),
                conversation.getSummary(),
                conversation.getCategory(),
                conversation.getUsecase(),
                conversation.getCreatedAt(),
                latestMessage.map(Message::getTimestamp).orElse(conversation.getCreatedAt()),
                messageCount
        );
    }

    private MessageHistoryItemResponse toMessageHistoryItem(Message message) {
        return new MessageHistoryItemResponse(
                message.getMessageId(),
                message.getConversationId(),
                message.getRole(),
                message.getContent(),
                message.getCategory(),
                message.getUsecase(),
                message.getTimestamp()
        );
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return Sort.by(Sort.Direction.ASC, "timestamp");
        }

        String[] parts = sort.split(",");
        String directionPart = parts.length > 1 ? parts[1].trim() : "asc";
        Sort.Direction direction = "desc".equalsIgnoreCase(directionPart)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return Sort.by(direction, "timestamp");
    }

    private int safePage(int page) {
        return Math.max(page, 0);
    }

    private int safeSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new RuntimeException("userId is required");
        }
    }
}

