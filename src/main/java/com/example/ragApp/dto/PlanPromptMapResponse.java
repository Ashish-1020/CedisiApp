package com.example.ragApp.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class PlanPromptMapResponse {

    private String planCode;
    private UUID promptId;
    private String category;
    private String useCase;
    private LocalDateTime mappedAt;

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

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

    public LocalDateTime getMappedAt() {
        return mappedAt;
    }

    public void setMappedAt(LocalDateTime mappedAt) {
        this.mappedAt = mappedAt;
    }
}

