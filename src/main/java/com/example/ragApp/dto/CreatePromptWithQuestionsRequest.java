package com.example.ragApp.dto;

import com.example.ragApp.helper.UseCaseType;
import java.util.List;

public class CreatePromptWithQuestionsRequest {

    private String category;
    private String useCase;
    private String systemPrompt;
    private String userPromptTemplate;

    private List<PriorQuestionDTO> priorQuestions;

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

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getUserPromptTemplate() {
        return userPromptTemplate;
    }

    public void setUserPromptTemplate(String userPromptTemplate) {
        this.userPromptTemplate = userPromptTemplate;
    }

    public List<PriorQuestionDTO> getPriorQuestions() {
        return priorQuestions;
    }

    public void setPriorQuestions(List<PriorQuestionDTO> priorQuestions) {
        this.priorQuestions = priorQuestions;
    }
}