package com.example.ragApp.controller;



import com.example.ragApp.dto.ChatRequest;
import com.example.ragApp.dto.ChatResponse;
import com.example.ragApp.helper.UseCaseType;
import com.example.ragApp.repository.ConversationRepository;
import com.example.ragApp.repository.MessageRepository;
import com.example.ragApp.repository.UserProfileRepository;
import com.example.ragApp.service.QueryService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.stringtemplate.v4.ST;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final QueryService chatService;
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;



    public ChatController(QueryService chatService, VectorStore vectorStore, EmbeddingModel embeddingModel) {
        this.chatService = chatService;
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
    }


    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse streamChat(@RequestBody ChatRequest request) {
        return chatService.streamChat(request);
    }


    @PostMapping(
            value = "/chat-with-file",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ChatResponse chatWithFile(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("userId") String userId,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam("message") String message,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam("category") String category,
            @RequestParam("usecase") String usecase
    ) {

        ChatRequest request = new ChatRequest(
                userId,
                conversationId,
                message,
                language,
                category,
                usecase
        );
        return chatService.chatWithMedia(files, request);
    }



}

