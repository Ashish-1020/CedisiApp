package com.example.ragApp.exception;

import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends AuthException {

    public InvalidPasswordException() {
        super("AUTH_INVALID_PASSWORD", "Invalid password", HttpStatus.UNAUTHORIZED);
    }
}

