package com.example.ragApp.controller;

import com.example.ragApp.dto.*;
import com.example.ragApp.service.PromptService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @PostMapping("/create")
    public UseCasePromptResponse createPromptWithQuestions(
            @RequestBody CreatePromptWithQuestionsRequest request) {
        return promptService.createPromptWithQuestions(request);
    }

    @GetMapping("/all-promptId")
    public List<PromptIdResponse> getAllPromptIds() {
        return promptService.getPromptIds();
    }

    @GetMapping("/categories")
    public List<CategoryUseCaseResponse> getCategories() {
        return promptService.getCategoriesWithUseCases();
    }

    @GetMapping("/questions/{useCase}")
    public List<PriorQuestionResponse> getQuestions(@PathVariable String useCase) {
        return promptService.getQuestionsByUseCase(useCase);
    }

    @GetMapping("/allowed/{planCode}")
    public com.example.ragApp.dto.PlanAllowedUseCasesResponse getAllowedUseCases(@PathVariable String planCode) {
        return promptService.getAllowedUseCasesByPlanCode(planCode);
    }
}