package com.zyc.copier_v0.modules.signal.ingest.service;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class Mt5SessionCacheSnapshot {

    private String sessionId;
    private String traceId;
    private Instant connectedAt;
    private Long login;
    private String server;
}
