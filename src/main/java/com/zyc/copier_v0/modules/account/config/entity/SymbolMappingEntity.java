package com.zyc.copier_v0.modules.account.config.entity;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
        name = "symbol_mappings",
        indexes = @Index(name = "idx_symbol_mapping_follower", columnList = "follower_account_id"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_symbol_mapping_follower_master_symbol",
                columnNames = {"follower_account_id", "master_symbol"}
        )
)
public class SymbolMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_account_id", nullable = false)
    private Mt5AccountEntity followerAccount;

    @Column(name = "master_symbol", nullable = false, length = 64)
    private String masterSymbol;

    @Column(name = "follower_symbol", nullable = false, length = 64)
    private String followerSymbol;

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

    public Mt5AccountEntity getFollowerAccount() {
        return followerAccount;
    }

    public void setFollowerAccount(Mt5AccountEntity followerAccount) {
        this.followerAccount = followerAccount;
    }

    public String getMasterSymbol() {
        return masterSymbol;
    }

    public void setMasterSymbol(String masterSymbol) {
        this.masterSymbol = masterSymbol;
    }

    public String getFollowerSymbol() {
        return followerSymbol;
    }

    public void setFollowerSymbol(String followerSymbol) {
        this.followerSymbol = followerSymbol;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
