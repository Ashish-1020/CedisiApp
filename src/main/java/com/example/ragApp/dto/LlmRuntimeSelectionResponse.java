package com.example.ragApp.dto;

public record LlmRuntimeSelectionResponse(String provider,
                                          String modelName,
                                          String source,
                                          boolean fallback) {
}

