package com.zyc.copier_v0.modules.signal.ingest.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

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

    public NormalizedMt5Signal(
            String traceId,
            String sessionId,
            Mt5SignalType type,
            String eventId,
            Long login,
            String server,
            String masterAccountKey,
            String sourceTimestamp,
            Instant receivedAt,
            JsonNode payload
    ) {
        this.traceId = traceId;
        this.sessionId = sessionId;
        this.type = type;
        this.eventId = eventId;
        this.login = login;
        this.server = server;
        this.masterAccountKey = masterAccountKey;
        this.sourceTimestamp = sourceTimestamp;
        this.receivedAt = receivedAt;
        this.payload = payload;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Mt5SignalType getType() {
        return type;
    }

    public String getEventId() {
        return eventId;
    }

    public Long getLogin() {
        return login;
    }

    public String getServer() {
        return server;
    }

    public String getMasterAccountKey() {
        return masterAccountKey;
    }

    public String getSourceTimestamp() {
        return sourceTimestamp;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public JsonNode getPayload() {
        return payload;
    }
}
