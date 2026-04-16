package com.example.ragApp.service;

import com.example.ragApp.data.PlanPromptMapping;
import com.example.ragApp.data.PlanPromptMappingId;
import com.example.ragApp.data.PromptTemplate;
import com.example.ragApp.data.SubscriptionPlan;
import com.example.ragApp.dto.PlanPromptMapRequest;
import com.example.ragApp.dto.PlanPromptMapResponse;
import com.example.ragApp.repository.PlanPromptMappingRepository;
import com.example.ragApp.repository.PromptTemplateRepository;
import com.example.ragApp.repository.SubscriptionPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PlanPromptMappingService {

    private final PlanPromptMappingRepository planPromptMappingRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PromptTemplateRepository promptTemplateRepository;

    public PlanPromptMappingService(PlanPromptMappingRepository planPromptMappingRepository,
                                    SubscriptionPlanRepository subscriptionPlanRepository,
                                    PromptTemplateRepository promptTemplateRepository) {
        this.planPromptMappingRepository = planPromptMappingRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.promptTemplateRepository = promptTemplateRepository;
    }

    @Transactional
    public PlanPromptMapResponse mapPromptToPlan(PlanPromptMapRequest request) {
        validateRequest(request);

        String normalizedPlanCode = request.getPlanCode().trim().toUpperCase(Locale.ROOT);
        if (planPromptMappingRepository.existsByIdPlanCodeIgnoreCaseAndIdPromptId(normalizedPlanCode, request.getPromptId())) {
            throw new RuntimeException("Prompt is already mapped to this plan");
        }

        SubscriptionPlan plan = getPlanByCode(normalizedPlanCode);
        PromptTemplate promptTemplate = getPromptById(request.getPromptId());

        PlanPromptMappingId mappingId = new PlanPromptMappingId();
        mappingId.setPlanCode(plan.getPlanCode());
        mappingId.setPromptId(promptTemplate.getId());

        PlanPromptMapping mapping = new PlanPromptMapping();
        mapping.setId(mappingId);
        mapping.setSubscriptionPlan(plan);
        mapping.setPromptTemplate(promptTemplate);

        return toResponse(planPromptMappingRepository.save(mapping));
    }

    @Transactional
    public void unmapPromptFromPlan(String planCode, UUID promptId) {
        if (planCode == null || planCode.trim().isEmpty()) {
            throw new RuntimeException("planCode is required");
        }
        if (promptId == null) {
            throw new RuntimeException("promptId is required");
        }

        long deleted = planPromptMappingRepository.deleteByIdPlanCodeIgnoreCaseAndIdPromptId(planCode.trim(), promptId);
        if (deleted == 0) {
            throw new RuntimeException("Mapping not found");
        }
    }

    @Transactional(readOnly = true)
    public List<PlanPromptMapResponse> getMappedPromptsByPlanCode(String planCode) {
        String normalizedPlanCode = normalizePlanCode(planCode);
        getPlanByCode(normalizedPlanCode);

        return planPromptMappingRepository.findByPlanCodeIgnoreCase(normalizedPlanCode)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlanPromptMapResponse> getAllowedUseCasesByPlanCode(String planCode) {
        String normalizedPlanCode = normalizePlanCode(planCode);

        SubscriptionPlan plan = getPlanByCode(normalizedPlanCode);
        if (!Boolean.TRUE.equals(plan.getActive())) {
            throw new RuntimeException("Subscription plan is inactive");
        }

        return planPromptMappingRepository.findByPlanCodeIgnoreCase(normalizedPlanCode)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateRequest(PlanPromptMapRequest request) {
        if (request == null) {
            throw new RuntimeException("request body is required");
        }
        normalizePlanCode(request.getPlanCode());
        if (request.getPromptId() == null) {
            throw new RuntimeException("promptId is required");
        }
    }

    private String normalizePlanCode(String planCode) {
        if (planCode == null || planCode.trim().isEmpty()) {
            throw new RuntimeException("planCode is required");
        }
        return planCode.trim().toUpperCase(Locale.ROOT);
    }

    private SubscriptionPlan getPlanByCode(String planCode) {
        return subscriptionPlanRepository.findByPlanCodeIgnoreCase(planCode)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));
    }

    private PromptTemplate getPromptById(UUID promptId) {
        return promptTemplateRepository.findById(promptId)
                .orElseThrow(() -> new RuntimeException("Prompt not found"));
    }

    private PlanPromptMapResponse toResponse(PlanPromptMapping mapping) {
        PlanPromptMapResponse response = new PlanPromptMapResponse();
        response.setPlanCode(mapping.getSubscriptionPlan().getPlanCode());
        response.setPromptId(mapping.getPromptTemplate().getId());
        response.setCategory(mapping.getPromptTemplate().getCategory());
        response.setUseCase(mapping.getPromptTemplate().getUseCase());
        response.setMappedAt(mapping.getCreatedAt());
        return response;
    }
}
