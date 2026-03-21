package com.zyc.copier_v0.modules.monitor.api;

import com.zyc.copier_v0.modules.monitor.domain.Mt5ConnectionStatus;
import java.time.Instant;

public class Mt5WsSessionResponse {

    private String sessionId;
    private String traceId;
    private Instant connectedAt;
    private Long accountId;
    private Long login;
    private String server;
    private String accountKey;
    private Mt5ConnectionStatus connectionStatus;
    private Instant lastHelloAt;
    private Instant lastHeartbeatAt;
    private Instant lastSignalAt;
    private String lastSignalType;
    private String lastEventId;

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

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
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
}
