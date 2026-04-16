package com.example.ragApp.service;

import com.example.ragApp.data.LlmModelConfig;
import com.example.ragApp.dto.LlmModelConfigRequest;
import com.example.ragApp.dto.LlmModelConfigResponse;
import com.example.ragApp.dto.LlmRuntimeSelectionResponse;
import com.example.ragApp.repository.LlmModelConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class LlmModelConfigService {

    private final LlmModelConfigRepository llmModelConfigRepository;
    private final Set<String> allowedProviders;
    private final String fallbackProvider;
    private final String fallbackModel;

    public LlmModelConfigService(
            LlmModelConfigRepository llmModelConfigRepository,
            @Value("${app.llm.allowed-providers:OPENAI,GEMINI,GROK,CLAUDE}") String allowedProvidersCsv,
            @Value("${app.llm.default-provider:OPENAI}") String fallbackProvider,
            @Value("${spring.ai.openai.chat.options.model:gpt-5.2}") String fallbackModel) {
        this.llmModelConfigRepository = llmModelConfigRepository;
        this.allowedProviders = parseCsvUpper(allowedProvidersCsv);
        this.fallbackProvider = normalizeUpper(fallbackProvider);
        this.fallbackModel = normalizeTrim(fallbackModel);
    }

    @Transactional
    public LlmModelConfigResponse createModel(LlmModelConfigRequest request) {
        validateCreateRequest(request);

        String provider = normalizeUpper(request.getProvider());
        String modelName = normalizeTrim(request.getModelName());

        ensureAllowedProvider(provider);

        if (llmModelConfigRepository.existsByProviderIgnoreCaseAndModelNameIgnoreCase(provider, modelName)) {
            throw new RuntimeException("Model config already exists for provider/model");
        }

        LlmModelConfig config = new LlmModelConfig();
        config.setProvider(provider);
        config.setModelName(modelName);
        config.setNotes(trimOrNull(request.getNotes()));
        config.setActive(false);
        config.setLastKnownGood(false);

        return toResponse(llmModelConfigRepository.save(config));
    }

    @Transactional(readOnly = true)
    public List<LlmModelConfigResponse> listModels() {
        return llmModelConfigRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LlmModelConfigResponse activateModel(Long id, String reason) {
        if (id == null) {
            throw new RuntimeException("modelConfigId is required");
        }

        LlmModelConfig config = llmModelConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Model config not found"));

        ensureAllowedProvider(config.getProvider());

        llmModelConfigRepository.deactivateAllActive();
        llmModelConfigRepository.clearLastKnownGoodFlag();

        config.setActive(true);
        config.setLastKnownGood(true);

        String note = trimOrNull(reason);
        if (note != null) {
            String current = trimOrNull(config.getNotes());
            String stamped = "[activated " + LocalDateTime.now() + "] " + note;
            config.setNotes(current == null ? stamped : current + " | " + stamped);
        }

        return toResponse(llmModelConfigRepository.save(config));
    }

    @Transactional
    public LlmModelConfigResponse rollbackToLastKnownGood(String reason) {
        LlmModelConfig lastKnownGood = llmModelConfigRepository.findFirstByLastKnownGoodTrueOrderByUpdatedAtDesc()
                .orElseThrow(() -> new RuntimeException("No lastKnownGood model found for rollback"));
        return activateModel(lastKnownGood.getId(), reason == null ? "rollback" : reason);
    }

    @Transactional(readOnly = true)
    public LlmRuntimeSelectionResponse getCurrentRuntimeSelection() {
        return resolveRuntimeSelection();
    }

    @Transactional(readOnly = true)
    public LlmRuntimeSelectionResponse resolveRuntimeSelection() {
        LlmRuntimeSelectionResponse selected = llmModelConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()
                .map(config -> new LlmRuntimeSelectionResponse(
                        config.getProvider(),
                        config.getModelName(),
                        "DB_ACTIVE",
                        false
                ))
                .or(() -> llmModelConfigRepository.findFirstByLastKnownGoodTrueOrderByUpdatedAtDesc()
                        .map(config -> new LlmRuntimeSelectionResponse(
                                config.getProvider(),
                                config.getModelName(),
                                "DB_LAST_KNOWN_GOOD",
                                true
                        )))
                .orElseGet(() -> new LlmRuntimeSelectionResponse(
                        fallbackProvider,
                        fallbackModel,
                        "APP_PROPERTIES",
                        true
                ));

        String provider = normalizeUpper(selected.provider());
        String modelName = normalizeTrim(selected.modelName());
        ensureAllowedProvider(provider);

        return new LlmRuntimeSelectionResponse(provider, modelName, selected.source(), selected.fallback());
    }

    private void validateCreateRequest(LlmModelConfigRequest request) {
        if (request == null) {
            throw new RuntimeException("request body is required");
        }
        if (isBlank(request.getProvider())) {
            throw new RuntimeException("provider is required");
        }
        if (isBlank(request.getModelName())) {
            throw new RuntimeException("modelName is required");
        }
    }

    private void ensureAllowedProvider(String provider) {
        if (!allowedProviders.contains(provider.toUpperCase(Locale.ROOT))) {
            throw new RuntimeException("provider is not allowed");
        }
    }

    private LlmModelConfigResponse toResponse(LlmModelConfig config) {
        LlmModelConfigResponse response = new LlmModelConfigResponse();
        response.setId(config.getId());
        response.setProvider(config.getProvider());
        response.setModelName(config.getModelName());
        response.setActive(config.getActive());
        response.setLastKnownGood(config.getLastKnownGood());
        response.setNotes(config.getNotes());
        response.setCreatedAt(config.getCreatedAt());
        response.setUpdatedAt(config.getUpdatedAt());
        return response;
    }

    private Set<String> parseCsvUpper(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return Set.of();
        }
        Set<String> values = new LinkedHashSet<>();
        Arrays.stream(csv.split(","))
                .map(this::normalizeUpper)
                .filter(v -> !v.isBlank())
                .forEach(values::add);
        return values;
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimOrNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}


