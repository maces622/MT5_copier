package com.zyc.copier_v0.modules.copy.followerexec.domain;

import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
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
