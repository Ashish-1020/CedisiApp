package com.example.ragApp.dto;

import java.util.List;

public class PlanAllowedUseCasesResponse {

    private String planCode;
    private List<AllowedPromptUseCaseItemResponse> prompts;

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public List<AllowedPromptUseCaseItemResponse> getPrompts() {
        return prompts;
    }

    public void setPrompts(List<AllowedPromptUseCaseItemResponse> prompts) {
        this.prompts = prompts;
    }
}
