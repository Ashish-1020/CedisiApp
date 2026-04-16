package com.example.ragApp.data;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    private String id;

    private String name;
    private String email;
    private String mobile;
    private String passwordHash;
    private String role; // USER / ADMIN
    private Boolean isVerified;

    private String userStatus; // ACTIVE / DEACTIVATED / DELETED
    private Integer noOfTokensConsumed;
    private String subscriptionStatus;
    private LocalDateTime registeredOn;
    private Long paymentId;
    private Integer noDeviceLoggedIn;
    private String subscriptionPlanId; // instead of only status
    private LocalDateTime subscriptionStart;
    private LocalDateTime subscriptionEnd;


    public String getSubscriptionPlanId() {
        return subscriptionPlanId;
    }

    public void setSubscriptionPlanId(String subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
    }

    public LocalDateTime getSubscriptionStart() {
        return subscriptionStart;
    }

    public void setSubscriptionStart(LocalDateTime subscriptionStart) {
        this.subscriptionStart = subscriptionStart;
    }

    public LocalDateTime getSubscriptionEnd() {
        return subscriptionEnd;
    }

    public void setSubscriptionEnd(LocalDateTime subscriptionEnd) {
        this.subscriptionEnd = subscriptionEnd;
    }

    public Integer getNoDeviceLoggedIn() {
        return noDeviceLoggedIn;
    }

    public void setNoDeviceLoggedIn(Integer noDeviceLoggedIn) {
        this.noDeviceLoggedIn = noDeviceLoggedIn;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public LocalDateTime getRegisteredOn() {
        return registeredOn;
    }

    public void setRegisteredOn(LocalDateTime registeredOn) {
        this.registeredOn = registeredOn;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public Integer getNoOfTokensConsumed() {
        return noOfTokensConsumed;
    }

    public void setNoOfTokensConsumed(Integer noOfTokensConsumed) {
        this.noOfTokensConsumed = noOfTokensConsumed;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    public Boolean getVerified() {
        return isVerified;
    }

    public void setVerified(Boolean verified) {
        isVerified = verified;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
