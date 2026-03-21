package com.zyc.copier_v0.modules.signal.ingest.domain;

import java.time.Instant;

public class Mt5SessionContext {

    private final String sessionId;
    private final String traceId;
    private final Instant connectedAt;
    private final Long login;
    private final String server;

    public Mt5SessionContext(String sessionId, String traceId, Instant connectedAt, Long login, String server) {
        this.sessionId = sessionId;
        this.traceId = traceId;
        this.connectedAt = connectedAt;
        this.login = login;
        this.server = server;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public Long getLogin() {
        return login;
    }

    public String getServer() {
        return server;
    }

    public Mt5SessionContext withAccount(Long login, String server) {
        return new Mt5SessionContext(sessionId, traceId, connectedAt, login, server);
    }

    public String masterAccountKey() {
        if (login == null || server == null || server.isBlank()) {
            return null;
        }
        return server + ":" + login;
    }
}
