package com.example.ragApp.data;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "plan_prompts")
public class PlanPromptMapping {

    @EmbeddedId
    private PlanPromptMappingId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_code", referencedColumnName = "plan_code", nullable = false, insertable = false, updatable = false)
    private SubscriptionPlan subscriptionPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prompt_id", nullable = false, insertable = false, updatable = false)
    private PromptTemplate promptTemplate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (id == null) {
            id = new PlanPromptMappingId();
        }
        if (id.getPlanCode() == null && subscriptionPlan != null) {
            id.setPlanCode(subscriptionPlan.getPlanCode());
        }
        if (id.getPromptId() == null && promptTemplate != null) {
            id.setPromptId(promptTemplate.getId());
        }
    }

    public PlanPromptMappingId getId() {
        return id;
    }

    public void setId(PlanPromptMappingId id) {
        this.id = id;
    }

    public SubscriptionPlan getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }

    public PromptTemplate getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(PromptTemplate promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
