package com.example.ragApp.dto;

import java.util.List;

public class CategoryUseCaseResponse {

    private String category;
    private List<String> useCases;

    public CategoryUseCaseResponse(String category, List<String> useCases) {
        this.category = category;
        this.useCases = useCases;
    }

    public String getCategory() { return category; }
    public List<String> getUseCases() { return useCases; }
}