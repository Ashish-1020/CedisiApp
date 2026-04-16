package com.example.ragApp.controller;

import com.example.ragApp.dto.ApiErrorResponse;
import com.example.ragApp.exception.PushTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PushTokenController.class)
public class PushTokenExceptionHandler {

    @ExceptionHandler(PushTokenException.class)
    public ResponseEntity<ApiErrorResponse> handlePushTokenException(PushTokenException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiErrorResponse(ex.getCode(), ex.getStatus().value(), ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(RuntimeException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse("PUSH_TOKEN_RUNTIME_EXCEPTION", status.value(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse("PUSH_TOKEN_INTERNAL_ERROR", status.value(), "Something went wrong while handling push token request."));
    }
}

