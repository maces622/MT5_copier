package com.zyc.copier_v0.modules.account.config.api;

import java.time.Instant;

public class SymbolMappingResponse {

    private Long id;
    private Long followerAccountId;
    private String masterSymbol;
    private String followerSymbol;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFollowerAccountId() {
        return followerAccountId;
    }

    public void setFollowerAccountId(Long followerAccountId) {
        this.followerAccountId = followerAccountId;
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
