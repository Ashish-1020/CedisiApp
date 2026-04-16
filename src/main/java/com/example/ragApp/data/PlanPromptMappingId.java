package com.example.ragApp.data;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class PlanPromptMappingId implements Serializable {

    @Column(name = "plan_code", nullable = false, length = 60)
    private String planCode;

    @Column(name = "prompt_id", nullable = false)
    private UUID promptId;

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public UUID getPromptId() {
        return promptId;
    }

    public void setPromptId(UUID promptId) {
        this.promptId = promptId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlanPromptMappingId that)) {
            return false;
        }
        return Objects.equals(planCode, that.planCode) && Objects.equals(promptId, that.promptId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(planCode, promptId);
    }
}

