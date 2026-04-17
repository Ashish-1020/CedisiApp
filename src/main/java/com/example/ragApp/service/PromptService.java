package com.example.ragApp.service;

import com.example.ragApp.data.PlanPromptMapping;
import com.example.ragApp.data.PriorQuestion;
import com.example.ragApp.data.PromptTemplate;
import com.example.ragApp.data.SubscriptionPlan;
import com.example.ragApp.dto.*;
import com.example.ragApp.repository.PlanPromptMappingRepository;
import com.example.ragApp.repository.PriorQuestionRepository;
import com.example.ragApp.repository.PromptTemplateRepository;
import com.example.ragApp.repository.SubscriptionPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PromptService {

    private final PromptTemplateRepository promptTemplateRepository;
    private final PriorQuestionRepository priorQuestionRepository;
    private final PlanPromptMappingRepository planPromptMappingRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public PromptService(PromptTemplateRepository promptTemplateRepository,
                         PriorQuestionRepository priorQuestionRepository,
                         PlanPromptMappingRepository planPromptMappingRepository,
                         SubscriptionPlanRepository subscriptionPlanRepository) {
        this.promptTemplateRepository = promptTemplateRepository;
        this.priorQuestionRepository = priorQuestionRepository;
        this.planPromptMappingRepository = planPromptMappingRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Transactional
    public UseCasePromptResponse createPromptWithQuestions(CreatePromptWithQuestionsRequest request) {

        PromptTemplate template = new PromptTemplate();
        template.setCategory(request.getCategory());
        template.setUseCase(request.getUseCase());
        template.setSystemPrompt(request.getSystemPrompt());
        template.setUserPromptTemplate(request.getUserPromptTemplate());
        template.setCreatedAt(LocalDateTime.now());

        template = promptTemplateRepository.save(template);

        List<PriorQuestionDTO> responseQuestions = new ArrayList<>();

        for (PriorQuestionDTO q : request.getPriorQuestions()) {

            PriorQuestion question = new PriorQuestion();
            question.setPromptTemplateId(template.getId());
            question.setQuestion(q.getQuestion());
            question.setQuestionKey(q.getKey());
            question.setQuestionType(q.getType());
            question.setCreatedAt(LocalDateTime.now());

            priorQuestionRepository.save(question);

            responseQuestions.add(q);
        }

        UseCasePromptResponse response = new UseCasePromptResponse();
        response.setCategory(template.getCategory());
        response.setUseCase(template.getUseCase());
        response.setSystemPrompt(template.getSystemPrompt());
        response.setUserPromptTemplate(template.getUserPromptTemplate());
        response.setPriorQuestions(responseQuestions);

        return response;
    }


    public List<CategoryUseCaseResponse> getCategoriesWithUseCases() {

        List<PromptTemplate> templates = promptTemplateRepository.findAll();

        Map<String, List<String>> map = new HashMap<>();

        for (PromptTemplate t : templates) {

            map.computeIfAbsent(t.getCategory(), k -> new ArrayList<>())
                    .add(t.getUseCase());
        }

        return map.entrySet()
                .stream()
                .map(e -> new CategoryUseCaseResponse(e.getKey(), e.getValue()))
                .toList();
    }

    /**
     * API 2
     * Fetch questions for a selected useCase
     */
    public List<PriorQuestionResponse> getQuestionsByUseCase(String useCase) {

        PromptTemplate template = promptTemplateRepository
                .findByUseCase(useCase)
                .orElseThrow(() -> new RuntimeException("UseCase not found"));

        List<PriorQuestion> questions =
                priorQuestionRepository.findByPromptTemplateIdOrderByQuestionOrder(template.getId());

        return questions.stream()
                .map(q -> new PriorQuestionResponse(
                        q.getQuestion(),
                        q.getQuestionKey(),
                        q.getQuestionType(),
                        q.getQuestionOrder()
                ))
                .toList();
    }



    public List<PromptIdResponse> getPromptIds() {

        List<PromptTemplate> templates = promptTemplateRepository.findAll();

        return templates.stream()
                .map(t -> new PromptIdResponse(
                        t.getId(),
                        t.getUseCase()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanAllowedUseCasesResponse getAllowedUseCasesByPlanCode(String planCode) {
        String normalizedPlanCode = normalizePlanCode(planCode);

        SubscriptionPlan plan = subscriptionPlanRepository.findByPlanCodeIgnoreCase(normalizedPlanCode)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        if (!Boolean.TRUE.equals(plan.getActive())) {
            throw new RuntimeException("Subscription plan is inactive");
        }

        List<PlanPromptMapping> mappings = planPromptMappingRepository.findByPlanCodeIgnoreCase(normalizedPlanCode);

        List<AllowedPromptUseCaseItemResponse> allowedPrompts = mappings.stream()
                .map(mapping -> {
                    AllowedPromptUseCaseItemResponse item = new AllowedPromptUseCaseItemResponse();
                    item.setPromptId(mapping.getPromptTemplate().getId());
                    item.setCategory(mapping.getPromptTemplate().getCategory());
                    item.setUseCase(mapping.getPromptTemplate().getUseCase());
                    return item;
                })
                .toList();

        PlanAllowedUseCasesResponse response = new PlanAllowedUseCasesResponse();
        response.setPlanCode(plan.getPlanCode());
        response.setPrompts(allowedPrompts);
        return response;
    }

    private String normalizePlanCode(String planCode) {
        if (planCode == null || planCode.trim().isEmpty()) {
            throw new RuntimeException("planCode is required");
        }
        return planCode.trim().toUpperCase(Locale.ROOT);
    }
}