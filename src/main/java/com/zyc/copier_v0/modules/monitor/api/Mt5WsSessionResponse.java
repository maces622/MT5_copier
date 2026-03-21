package com.zyc.copier_v0.modules.monitor.api;

import com.zyc.copier_v0.modules.monitor.domain.Mt5ConnectionStatus;
import java.time.Instant;
import lombok.Data;

@Data
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
}
