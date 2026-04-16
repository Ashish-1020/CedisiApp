package com.example.ragApp.service;

import com.example.ragApp.data.SubscriptionPlan;
import com.example.ragApp.dto.SubscriptionPlanRequest;
import com.example.ragApp.dto.SubscriptionPlanResponse;
import com.example.ragApp.repository.SubscriptionPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public SubscriptionPlanService(SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Transactional
    public SubscriptionPlanResponse createPlan(SubscriptionPlanRequest request) {
        validateRequest(request);

        String planCode = request.getPlanCode().trim().toUpperCase(Locale.ROOT);
        if (subscriptionPlanRepository.existsByPlanCodeIgnoreCase(planCode)) {
            throw new RuntimeException("planCode already exists");
        }

        SubscriptionPlan plan = new SubscriptionPlan();
        applyValues(plan, request);
        return toResponse(subscriptionPlanRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getAllPlans() {
        return subscriptionPlanRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getActivePlans() {
        return subscriptionPlanRepository.findByActiveTrueOrderByCostAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlanById(Long id) {
        return toResponse(getEntityById(id));
    }

    @Transactional
    public SubscriptionPlanResponse updatePlan(Long id, SubscriptionPlanRequest request) {
        validateRequest(request);
        SubscriptionPlan plan = getEntityById(id);

        String incomingPlanCode = request.getPlanCode().trim().toUpperCase(Locale.ROOT);
        if (!plan.getPlanCode().equalsIgnoreCase(incomingPlanCode)
                && subscriptionPlanRepository.existsByPlanCodeIgnoreCase(incomingPlanCode)) {
            throw new RuntimeException("planCode already exists");
        }

        applyValues(plan, request);
        return toResponse(subscriptionPlanRepository.save(plan));
    }

    @Transactional
    public void deactivatePlan(Long id) {
        SubscriptionPlan plan = getEntityById(id);
        plan.setActive(false);
        subscriptionPlanRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public SubscriptionPlan resolveActivePlanByCode(String planCode) {
        if (planCode == null || planCode.trim().isEmpty()) {
            throw new RuntimeException("planId is required");
        }

        SubscriptionPlan plan = subscriptionPlanRepository.findByPlanCodeIgnoreCase(planCode.trim())
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        if (!Boolean.TRUE.equals(plan.getActive())) {
            throw new RuntimeException("Subscription plan is inactive");
        }
        return plan;
    }

    private SubscriptionPlan getEntityById(Long id) {
        if (id == null) {
            throw new RuntimeException("planId is required");
        }

        return subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));
    }

    private void validateRequest(SubscriptionPlanRequest request) {
        if (request == null) {
            throw new RuntimeException("request body is required");
        }
        if (request.getPlanName() == null || request.getPlanName().trim().isEmpty()) {
            throw new RuntimeException("planName is required");
        }
        if (request.getPlanCode() == null || request.getPlanCode().trim().isEmpty()) {
            throw new RuntimeException("planCode is required");
        }
        if (request.getExpirationDays() == null || request.getExpirationDays() <= 0) {
            throw new RuntimeException("expirationDays must be positive");
        }
        if (request.getCost() == null || request.getCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("cost must be zero or positive");
        }
        if (request.getTokenLimit() == null || request.getTokenLimit() < 0) {
            throw new RuntimeException("tokenLimit must be zero or positive");
        }
    }

    private void applyValues(SubscriptionPlan plan, SubscriptionPlanRequest request) {
        plan.setPlanName(request.getPlanName().trim());
        plan.setPlanCode(request.getPlanCode().trim().toUpperCase(Locale.ROOT));
        plan.setExpirationDays(request.getExpirationDays());
        plan.setCost(request.getCost());
        plan.setTokenLimit(request.getTokenLimit());
        plan.setActive(request.getActive() == null ? true : request.getActive());
    }

    private SubscriptionPlanResponse toResponse(SubscriptionPlan plan) {
        SubscriptionPlanResponse response = new SubscriptionPlanResponse();
        response.setId(plan.getId());
        response.setPlanName(plan.getPlanName());
        response.setPlanCode(plan.getPlanCode());
        response.setExpirationDays(plan.getExpirationDays());
        response.setCost(plan.getCost());
        response.setTokenLimit(plan.getTokenLimit());
        response.setActive(plan.getActive());
        return response;
    }
}

