package com.zyc.copier_v0.modules.copy.engine.entity;

import com.zyc.copier_v0.modules.copy.engine.domain.FollowerDispatchStatus;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(
        name = "follower_dispatch_outbox",
        indexes = {
                @Index(name = "idx_dispatch_follower_status", columnList = "follower_account_id,status"),
                @Index(name = "idx_dispatch_master_event", columnList = "master_event_id")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_dispatch_command", columnNames = {"execution_command_id"})
)
public class FollowerDispatchOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_command_id", nullable = false)
    private Long executionCommandId;

    @Column(name = "master_event_id", nullable = false, length = 128)
    private String masterEventId;

    @Column(name = "follower_account_id", nullable = false)
    private Long followerAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private FollowerDispatchStatus status;

    @Column(name = "status_message", length = 255)
    private String statusMessage;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Column(name = "acked_at")
    private Instant ackedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
