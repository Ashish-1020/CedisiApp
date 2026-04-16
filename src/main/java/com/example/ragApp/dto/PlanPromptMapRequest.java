package com.example.ragApp.dto;

import java.util.UUID;

public class PlanPromptMapRequest {

    private String planCode;
    private UUID promptId;

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
}

