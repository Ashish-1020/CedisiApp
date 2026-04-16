package com.example.ragApp.repository;

import com.example.ragApp.data.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository
        extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByTimestampAsc(UUID conversationId);

    List<Message> findTop6ByConversationIdOrderByTimestampDesc(UUID conversationId);

    Page<Message> findByConversationId(UUID conversationId, Pageable pageable);

    Optional<Message> findTopByConversationIdOrderByTimestampDesc(UUID conversationId);

    long countByConversationId(UUID conversationId);

}
