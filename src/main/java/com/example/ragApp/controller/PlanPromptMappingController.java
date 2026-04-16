package com.example.ragApp.controller;

import com.example.ragApp.dto.ApiMessageResponse;
import com.example.ragApp.dto.PlanPromptMapRequest;
import com.example.ragApp.dto.PlanPromptMapResponse;
import com.example.ragApp.service.PlanPromptMappingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/plan-prompts")
public class PlanPromptMappingController {

    private final PlanPromptMappingService planPromptMappingService;

    public PlanPromptMappingController(PlanPromptMappingService planPromptMappingService) {
        this.planPromptMappingService = planPromptMappingService;
    }

    @PostMapping("/admin")
    public ResponseEntity<PlanPromptMapResponse> mapPromptToPlan(@RequestBody PlanPromptMapRequest request) {
        return ResponseEntity.ok(planPromptMappingService.mapPromptToPlan(request));
    }

    @DeleteMapping("/admin/{planCode}/{promptId}")
    public ResponseEntity<ApiMessageResponse> unmapPromptFromPlan(@PathVariable String planCode,
                                                                  @PathVariable UUID promptId) {
        planPromptMappingService.unmapPromptFromPlan(planCode, promptId);
        return ResponseEntity.ok(new ApiMessageResponse("Prompt unmapped from plan"));
    }

    @GetMapping("/admin/{planCode}")
    public ResponseEntity<List<PlanPromptMapResponse>> getMappedPromptsByPlanCode(@PathVariable String planCode) {
        return ResponseEntity.ok(planPromptMappingService.getMappedPromptsByPlanCode(planCode));
    }
}

