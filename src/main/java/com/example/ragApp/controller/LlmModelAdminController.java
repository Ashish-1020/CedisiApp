package com.example.ragApp.controller;

import com.example.ragApp.dto.LlmModelConfigRequest;
import com.example.ragApp.dto.LlmModelConfigResponse;
import com.example.ragApp.dto.LlmModelSwitchRequest;
import com.example.ragApp.dto.LlmRuntimeSelectionResponse;
import com.example.ragApp.service.LlmModelConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/llm-models")
public class LlmModelAdminController {

    private final LlmModelConfigService llmModelConfigService;
    private final String adminApiKey;

    public LlmModelAdminController(LlmModelConfigService llmModelConfigService,
                                   @Value("${app.admin.api-key:}") String adminApiKey) {
        this.llmModelConfigService = llmModelConfigService;
        this.adminApiKey = adminApiKey;
    }

    @PostMapping("/admin")
    public ResponseEntity<LlmModelConfigResponse> createModel(@RequestHeader(value = "X-Admin-Key", required = false) String incomingAdminKey,
                                                               @RequestBody LlmModelConfigRequest request) {
        verifyAdminKey(incomingAdminKey);
        return ResponseEntity.ok(llmModelConfigService.createModel(request));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<LlmModelConfigResponse>> listModels(@RequestHeader(value = "X-Admin-Key", required = false) String incomingAdminKey) {
        verifyAdminKey(incomingAdminKey);
        return ResponseEntity.ok(llmModelConfigService.listModels());
    }

    @GetMapping("/admin/current")
    public ResponseEntity<LlmRuntimeSelectionResponse> getCurrentModel(@RequestHeader(value = "X-Admin-Key", required = false) String incomingAdminKey) {
        verifyAdminKey(incomingAdminKey);
        return ResponseEntity.ok(llmModelConfigService.getCurrentRuntimeSelection());
    }

    @PutMapping("/admin/{id}/activate")
    public ResponseEntity<LlmModelConfigResponse> activateModel(@RequestHeader(value = "X-Admin-Key", required = false) String incomingAdminKey,
                                                                 @PathVariable Long id,
                                                                 @RequestBody(required = false) LlmModelSwitchRequest request) {
        verifyAdminKey(incomingAdminKey);
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(llmModelConfigService.activateModel(id, reason));
    }

    @PostMapping("/admin/rollback")
    public ResponseEntity<LlmModelConfigResponse> rollback(@RequestHeader(value = "X-Admin-Key", required = false) String incomingAdminKey,
                                                            @RequestBody(required = false) LlmModelSwitchRequest request) {
        verifyAdminKey(incomingAdminKey);
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(llmModelConfigService.rollbackToLastKnownGood(reason));
    }

    private void verifyAdminKey(String incomingAdminKey) {
        if (adminApiKey == null || adminApiKey.isBlank()) {
            throw new RuntimeException("Server admin API key is not configured");
        }
        if (!adminApiKey.equals(incomingAdminKey)) {
            throw new RuntimeException("Invalid admin API key");
        }
    }
}

