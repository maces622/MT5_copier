package com.zyc.copier_v0.modules.copy.followerexec.api;

import com.zyc.copier_v0.modules.monitor.domain.Mt5ConnectionStatus;
import java.time.Instant;

public class FollowerExecSessionResponse {

    private String sessionId;
    private String traceId;
    private Instant connectedAt;
    private Long followerAccountId;
    private Long login;
    private String server;
    private String accountKey;
    private Mt5ConnectionStatus connectionStatus;
    private Instant lastHelloAt;
    private Instant lastHeartbeatAt;
    private Instant lastDispatchSentAt;
    private Long lastDispatchId;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(Instant connectedAt) {
        this.connectedAt = connectedAt;
    }

    public Long getFollowerAccountId() {
        return followerAccountId;
    }

    public void setFollowerAccountId(Long followerAccountId) {
        this.followerAccountId = followerAccountId;
    }

    public Long getLogin() {
        return login;
    }

    public void setLogin(Long login) {
        this.login = login;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getAccountKey() {
        return accountKey;
    }

    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
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

    public Instant getLastDispatchSentAt() {
        return lastDispatchSentAt;
    }

    public void setLastDispatchSentAt(Instant lastDispatchSentAt) {
        this.lastDispatchSentAt = lastDispatchSentAt;
    }

    public Long getLastDispatchId() {
        return lastDispatchId;
    }

    public void setLastDispatchId(Long lastDispatchId) {
        this.lastDispatchId = lastDispatchId;
    }
}
