package com.example.ragApp.dto;


public record ChatResponse(
        String conversationId,
        String response,
        String followUp
) {}
