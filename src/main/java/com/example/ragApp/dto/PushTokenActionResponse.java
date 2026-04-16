package com.example.ragApp.dto;

public class PushTokenActionResponse {

    private String code;
    private int status;
    private String message;
    private PushTokenRecordResponse token;

    public PushTokenActionResponse() {
    }

    public PushTokenActionResponse(String code, int status, String message, PushTokenRecordResponse token) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.token = token;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public PushTokenRecordResponse getToken() {
        return token;
    }

    public void setToken(PushTokenRecordResponse token) {
        this.token = token;
    }
}

