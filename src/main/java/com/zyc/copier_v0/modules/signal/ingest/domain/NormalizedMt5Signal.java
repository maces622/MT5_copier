package com.zyc.copier_v0.modules.signal.ingest.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NormalizedMt5Signal {

    private final String traceId;
    private final String sessionId;
    private final Mt5SignalType type;
    private final String eventId;
    private final Long login;
    private final String server;
    private final String masterAccountKey;
    private final String sourceTimestamp;
    private final Instant receivedAt;
    private final JsonNode payload;
}
