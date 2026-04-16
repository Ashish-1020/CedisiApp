package com.example.ragApp.controller;

import com.example.ragApp.dto.UserProfileErrorResponse;
import com.example.ragApp.exception.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UserProfileController.class)
public class UserProfileExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<UserProfileErrorResponse> handleUserProfileException(AuthException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new UserProfileErrorResponse(ex.getCode(), ex.getStatus().value(), ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<UserProfileErrorResponse> handleRuntimeException(RuntimeException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new UserProfileErrorResponse("USER_PROFILE_RUNTIME_EXCEPTION", status.value(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<UserProfileErrorResponse> handleUnexpectedException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(new UserProfileErrorResponse(
                        "USER_PROFILE_INTERNAL_ERROR",
                        status.value(),
                        "Something went wrong while processing user profile request."
                ));
    }
}

