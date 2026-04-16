package com.example.ragApp.dto;

public record UserProfileErrorResponse(
        String code,
        int status,
        String message
) {
}

