package com.zyc.copier_v0.modules.signal.ingest.domain;

import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Mt5SessionContext {

    private final String sessionId;
    private final String traceId;
    private final Instant connectedAt;
    private final Long login;
    private final String server;

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
