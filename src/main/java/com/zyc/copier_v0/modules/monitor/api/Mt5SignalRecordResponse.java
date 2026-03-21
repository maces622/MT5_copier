package com.zyc.copier_v0.modules.monitor.api;

import java.time.Instant;
import lombok.Data;

@Data
public class Mt5SignalRecordResponse {

    private Long id;
    private String eventId;
    private String signalType;
    private Long accountId;
    private Long login;
    private String server;
    private String accountKey;
    private String sourceTimestamp;
    private Instant receivedAt;
    private String payloadJson;
}
