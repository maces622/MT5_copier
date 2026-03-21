package com.zyc.copier_v0.modules.monitor.api;

import com.zyc.copier_v0.modules.account.config.domain.AccountStatus;
import com.zyc.copier_v0.modules.account.config.domain.Mt5AccountRole;
import com.zyc.copier_v0.modules.monitor.domain.Mt5ConnectionStatus;
import java.time.Instant;

public class Mt5AccountMonitorOverviewResponse {

    private Long accountId;
    private Long userId;
    private String brokerName;
    private String serverName;
    private Long mt5Login;
    private String accountKey;
    private Mt5AccountRole accountRole;
    private AccountStatus accountStatus;
    private Mt5ConnectionStatus connectionStatus;
    private Instant lastHelloAt;
    private Instant lastHeartbeatAt;
    private Instant lastSignalAt;
    private String lastSignalType;
    private String lastEventId;
    private Long activeFollowerCount;
    private Long activeMasterCount;
    private Long pendingDispatchCount;
    private Long failedDispatchCount;
    private Instant updatedAt;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
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

    public String getAccountKey() {
        return accountKey;
    }

    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
    }

    public Mt5AccountRole getAccountRole() {
        return accountRole;
    }

    public void setAccountRole(Mt5AccountRole accountRole) {
        this.accountRole = accountRole;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public Mt5ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(Mt5ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public Instant getLastHelloAt() {
        return lastHelloAt;
    }

    public void setLastHelloAt(Instant lastHelloAt) {
        this.lastHelloAt = lastHelloAt;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public Instant getLastSignalAt() {
        return lastSignalAt;
    }

    public void setLastSignalAt(Instant lastSignalAt) {
        this.lastSignalAt = lastSignalAt;
    }

    public String getLastSignalType() {
        return lastSignalType;
    }

    public void setLastSignalType(String lastSignalType) {
        this.lastSignalType = lastSignalType;
    }

    public String getLastEventId() {
        return lastEventId;
    }

    public void setLastEventId(String lastEventId) {
        this.lastEventId = lastEventId;
    }

    public Long getActiveFollowerCount() {
        return activeFollowerCount;
    }

    public void setActiveFollowerCount(Long activeFollowerCount) {
        this.activeFollowerCount = activeFollowerCount;
    }

    public Long getActiveMasterCount() {
        return activeMasterCount;
    }

    public void setActiveMasterCount(Long activeMasterCount) {
        this.activeMasterCount = activeMasterCount;
    }

    public Long getPendingDispatchCount() {
        return pendingDispatchCount;
    }

    public void setPendingDispatchCount(Long pendingDispatchCount) {
        this.pendingDispatchCount = pendingDispatchCount;
    }

    public Long getFailedDispatchCount() {
        return failedDispatchCount;
    }

    public void setFailedDispatchCount(Long failedDispatchCount) {
        this.failedDispatchCount = failedDispatchCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
