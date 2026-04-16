package com.example.ragApp.dto;

import java.util.UUID;

public class AllowedPromptUseCaseItemResponse {

    private UUID promptId;
    private String category;
    private String useCase;

    public UUID getPromptId() {
        return promptId;
    }

    public void setPromptId(UUID promptId) {
        this.promptId = promptId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUseCase() {
        return useCase;
    }

    public void setUseCase(String useCase) {
        this.useCase = useCase;
    }
}

