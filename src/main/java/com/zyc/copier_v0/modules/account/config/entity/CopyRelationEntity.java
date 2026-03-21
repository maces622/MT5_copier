package com.zyc.copier_v0.modules.account.config.entity;

import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
        name = "copy_relations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_copy_relation_master_follower",
                columnNames = {"master_account_id", "follower_account_id"}
        )
)
public class CopyRelationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "master_account_id", nullable = false)
    private Mt5AccountEntity masterAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_account_id", nullable = false)
    private Mt5AccountEntity followerAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "copy_mode", nullable = false, length = 32)
    private CopyMode copyMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CopyRelationStatus status;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "config_version", nullable = false)
    private Long configVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    public Mt5AccountEntity getMasterAccount() {
        return masterAccount;
    }

    public void setMasterAccount(Mt5AccountEntity masterAccount) {
        this.masterAccount = masterAccount;
    }

    public Mt5AccountEntity getFollowerAccount() {
        return followerAccount;
    }

    public void setFollowerAccount(Mt5AccountEntity followerAccount) {
        this.followerAccount = followerAccount;
    }

    public CopyMode getCopyMode() {
        return copyMode;
    }

    public void setCopyMode(CopyMode copyMode) {
        this.copyMode = copyMode;
    }

    public CopyRelationStatus getStatus() {
        return status;
    }

    public void setStatus(CopyRelationStatus status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Long getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(Long configVersion) {
        this.configVersion = configVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
