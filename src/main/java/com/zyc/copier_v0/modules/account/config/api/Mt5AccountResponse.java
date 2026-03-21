package com.zyc.copier_v0.modules.account.config.api;

import com.zyc.copier_v0.modules.account.config.domain.AccountStatus;
import com.zyc.copier_v0.modules.account.config.domain.Mt5AccountRole;
import java.time.Instant;

public class Mt5AccountResponse {

    private Long id;
    private Long userId;
    private String brokerName;
    private String serverName;
    private Long mt5Login;
    private Integer credentialVersion;
    private boolean credentialConfigured;
    private Mt5AccountRole accountRole;
    private AccountStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public Long getMt5Login() {
        return mt5Login;
    }

    public void setMt5Login(Long mt5Login) {
        this.mt5Login = mt5Login;
    }

    public Integer getCredentialVersion() {
        return credentialVersion;
    }

    public void setCredentialVersion(Integer credentialVersion) {
        this.credentialVersion = credentialVersion;
    }

    public boolean isCredentialConfigured() {
        return credentialConfigured;
    }

    public void setCredentialConfigured(boolean credentialConfigured) {
        this.credentialConfigured = credentialConfigured;
    }

    public Mt5AccountRole getAccountRole() {
        return accountRole;
    }

    public void setAccountRole(Mt5AccountRole accountRole) {
        this.accountRole = accountRole;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
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
