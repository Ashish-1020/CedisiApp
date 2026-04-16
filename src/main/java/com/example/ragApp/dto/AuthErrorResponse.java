package com.example.ragApp.dto;

public record AuthErrorResponse(
		String code,
		int status,
		String message
) {
}

