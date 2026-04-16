package com.example.ragApp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatRequest(
        @JsonProperty("userId") String userId,
        @JsonProperty("conversationId") String conversationId, // null for first time
        @JsonProperty("message") String message,
        @JsonProperty("language") String language,
        @JsonProperty("category") String category,
        @JsonProperty("usecase") @JsonAlias({"useCase", "use_case"}) String usecase
) {}

