package com.zyc.copier_v0.modules.copy.followerexec.service;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class FollowerExecSessionCacheSnapshot {

    private String sessionId;
    private String traceId;
    private Instant connectedAt;
    private Long followerAccountId;
    private Long login;
    private String server;
    private Instant lastHelloAt;
    private Instant lastHeartbeatAt;
    private Instant lastDispatchSentAt;
    private Long lastDispatchId;
}
