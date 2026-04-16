package com.example.ragApp.controller;

import com.example.ragApp.dto.ApiMessageResponse;
import com.example.ragApp.dto.SubscriptionPlanRequest;
import com.example.ragApp.dto.SubscriptionPlanResponse;
import com.example.ragApp.service.SubscriptionPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscription-plans")
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    public SubscriptionPlanController(SubscriptionPlanService subscriptionPlanService) {
        this.subscriptionPlanService = subscriptionPlanService;
    }

    @PostMapping("/admin")
    public ResponseEntity<SubscriptionPlanResponse> createPlan(@RequestBody SubscriptionPlanRequest request) {
        return ResponseEntity.ok(subscriptionPlanService.createPlan(request));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<SubscriptionPlanResponse>> getAllPlansForAdmin() {
        return ResponseEntity.ok(subscriptionPlanService.getAllPlans());
    }

    @GetMapping("/admin/{planId}")
    public ResponseEntity<SubscriptionPlanResponse> getPlanForAdmin(@PathVariable Long planId) {
        return ResponseEntity.ok(subscriptionPlanService.getPlanById(planId));
    }

    @PutMapping("/admin/{planId}")
    public ResponseEntity<SubscriptionPlanResponse> updatePlan(@PathVariable Long planId,
                                                               @RequestBody SubscriptionPlanRequest request) {
        return ResponseEntity.ok(subscriptionPlanService.updatePlan(planId, request));
    }

    @DeleteMapping("/admin/{planId}")
    public ResponseEntity<ApiMessageResponse> deletePlan(@PathVariable Long planId) {
        subscriptionPlanService.deactivatePlan(planId);
        return ResponseEntity.ok(new ApiMessageResponse("Subscription plan deactivated"));
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionPlanResponse>> getActivePlans() {
        return ResponseEntity.ok(subscriptionPlanService.getActivePlans());
    }
}

