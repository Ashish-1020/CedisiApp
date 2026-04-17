package com.example.ragApp.dto;

import java.util.UUID;

public class PromptIdResponse {
    private UUID promptId;
    private String useCase;

    public PromptIdResponse(UUID promptId, String useCase) {
        this.promptId = promptId;
        this.useCase = useCase;
    }

    public String getUseCase() {
        return useCase;
    }

    public void setUseCase(String useCase) {
        this.useCase = useCase;
    }

    public UUID getPromptId() {
        return promptId;
    }

    public void setPromptId(UUID promptId) {
        this.promptId = promptId;
    }
}
