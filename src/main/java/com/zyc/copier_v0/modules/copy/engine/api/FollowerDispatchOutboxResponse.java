package com.zyc.copier_v0.modules.copy.engine.api;

import com.zyc.copier_v0.modules.copy.engine.domain.FollowerDispatchStatus;
import java.time.Instant;

public class FollowerDispatchOutboxResponse {

    private Long id;
    private Long executionCommandId;
    private String masterEventId;
    private Long followerAccountId;
    private FollowerDispatchStatus status;
    private String statusMessage;
    private String payloadJson;
    private Instant ackedAt;
    private Instant failedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExecutionCommandId() {
        return executionCommandId;
    }

    public void setExecutionCommandId(Long executionCommandId) {
        this.executionCommandId = executionCommandId;
    }

    public String getMasterEventId() {
        return masterEventId;
    }

    public void setMasterEventId(String masterEventId) {
        this.masterEventId = masterEventId;
    }

    public Long getFollowerAccountId() {
        return followerAccountId;
    }

    public void setFollowerAccountId(Long followerAccountId) {
        this.followerAccountId = followerAccountId;
    }

    public FollowerDispatchStatus getStatus() {
        return status;
    }

    public void setStatus(FollowerDispatchStatus status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public Instant getAckedAt() {
        return ackedAt;
    }

    public void setAckedAt(Instant ackedAt) {
        this.ackedAt = ackedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
