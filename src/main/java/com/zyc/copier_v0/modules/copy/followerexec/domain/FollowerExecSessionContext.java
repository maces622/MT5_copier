package com.zyc.copier_v0.modules.copy.followerexec.domain;

import java.time.Instant;

public class FollowerExecSessionContext {

    private final String sessionId;
    private final String traceId;
    private final Instant connectedAt;
    private final Long followerAccountId;
    private final Long login;
    private final String server;
    private final Instant lastHelloAt;
    private final Instant lastHeartbeatAt;
    private final Instant lastDispatchSentAt;
    private final Long lastDispatchId;

    public FollowerExecSessionContext(
            String sessionId,
            String traceId,
            Instant connectedAt,
            Long followerAccountId,
            Long login,
            String server,
            Instant lastHelloAt,
            Instant lastHeartbeatAt,
            Instant lastDispatchSentAt,
            Long lastDispatchId
    ) {
        this.sessionId = sessionId;
        this.traceId = traceId;
        this.connectedAt = connectedAt;
        this.followerAccountId = followerAccountId;
        this.login = login;
        this.server = server;
        this.lastHelloAt = lastHelloAt;
        this.lastHeartbeatAt = lastHeartbeatAt;
        this.lastDispatchSentAt = lastDispatchSentAt;
        this.lastDispatchId = lastDispatchId;
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

    public Long getFollowerAccountId() {
        return followerAccountId;
    }

    public Long getLogin() {
        return login;
    }

    public String getServer() {
        return server;
    }

    public Instant getLastHelloAt() {
        return lastHelloAt;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public Instant getLastDispatchSentAt() {
        return lastDispatchSentAt;
    }

    public Long getLastDispatchId() {
        return lastDispatchId;
    }

    public FollowerExecSessionContext withFollower(Long followerAccountId, Long login, String server, Instant at) {
        return new FollowerExecSessionContext(
                sessionId,
                traceId,
                connectedAt,
                followerAccountId,
                login,
                server,
                at,
                lastHeartbeatAt,
                lastDispatchSentAt,
                lastDispatchId
        );
    }

    public FollowerExecSessionContext withHeartbeat(Instant at) {
        return new FollowerExecSessionContext(
                sessionId,
                traceId,
                connectedAt,
                followerAccountId,
                login,
                server,
                lastHelloAt,
                at,
                lastDispatchSentAt,
                lastDispatchId
        );
    }

    public FollowerExecSessionContext withDispatch(Instant at, Long dispatchId) {
        return new FollowerExecSessionContext(
                sessionId,
                traceId,
                connectedAt,
                followerAccountId,
                login,
                server,
                lastHelloAt,
                lastHeartbeatAt,
                at,
                dispatchId
        );
    }

    public String accountKey() {
        if (login == null || server == null || server.isBlank()) {
            return null;
        }
        return server + ":" + login;
    }
}
