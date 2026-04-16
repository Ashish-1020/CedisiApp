package com.example.ragApp.dto;

import java.time.LocalDateTime;

public class LoginResponse {

    private String message;
    private String email;
    private String role;
    private String id;
    private String subscription_status;
    private String subscription_plan_id;
    private LocalDateTime subscription_expiration;
    private Integer token_limit;
    private Integer tokens_consumed;


    public LoginResponse(String message, String email, String role, String id, String subscription_status,
                         String subscription_plan_id, LocalDateTime subscription_expiration,
                         Integer token_limit, Integer tokens_consumed) {
        this.message = message;
        this.email = email;
        this.role = role;
        this.id=id;
        this.subscription_status=subscription_status;
        this.subscription_plan_id=subscription_plan_id;
        this.subscription_expiration=subscription_expiration;
        this.token_limit = token_limit;
        this.tokens_consumed = tokens_consumed;
    }

    public Integer getToken_limit() {
        return token_limit;
    }

    public void setToken_limit(Integer token_limit) {
        this.token_limit = token_limit;
    }

    public Integer getTokens_consumed() {
        return tokens_consumed;
    }

    public void setTokens_consumed(Integer tokens_consumed) {
        this.tokens_consumed = tokens_consumed;
    }

    public String getSubscription_status() {
        return subscription_status;
    }

    public void setSubscription_status(String subscription_status) {
        this.subscription_status = subscription_status;
    }

    public String getSubscription_plan_id() {
        return subscription_plan_id;
    }

    public void setSubscription_plan_id(String subscription_plan_id) {
        this.subscription_plan_id = subscription_plan_id;
    }

    public LocalDateTime getSubscription_expiration() {
        return subscription_expiration;
    }

    public void setSubscription_expiration(LocalDateTime subscription_expiration) {
        this.subscription_expiration = subscription_expiration;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
