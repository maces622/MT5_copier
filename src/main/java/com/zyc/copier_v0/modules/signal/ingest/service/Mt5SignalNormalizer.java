package com.zyc.copier_v0.modules.signal.ingest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyc.copier_v0.modules.signal.ingest.domain.Mt5SessionContext;
import com.zyc.copier_v0.modules.signal.ingest.domain.Mt5SignalType;
import com.zyc.copier_v0.modules.signal.ingest.domain.NormalizedMt5Signal;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class Mt5SignalNormalizer {

    public NormalizedMt5Signal normalize(
            JsonNode payload,
            String sessionId,
            String traceId,
            Instant receivedAt,
            Mt5SessionContext sessionContext
    ) {
        Mt5SignalType type = Mt5SignalType.fromCode(payload.path("type").asText(null));
        Long login = resolveLogin(payload, sessionContext);
        String server = resolveServer(payload, sessionContext);
        String sourceTimestamp = resolveSourceTimestamp(payload);
        String masterAccountKey = resolveMasterAccountKey(login, server, sessionId);
        String eventId = resolveEventId(payload, type, masterAccountKey, sessionId, sourceTimestamp, receivedAt);

        return new NormalizedMt5Signal(
                traceId,
                sessionId,
                type,
                eventId,
                login,
                server,
                masterAccountKey,
                sourceTimestamp,
                receivedAt,
                payload.deepCopy()
        );
    }

    private Long resolveLogin(JsonNode payload, Mt5SessionContext sessionContext) {
        if (payload.hasNonNull("login")) {
            return payload.path("login").asLong();
        }
        if (sessionContext != null) {
            return sessionContext.getLogin();
        }
        return null;
    }

    private String resolveServer(JsonNode payload, Mt5SessionContext sessionContext) {
        if (payload.hasNonNull("server")) {
            String server = payload.path("server").asText();
            if (StringUtils.hasText(server)) {
                return server;
            }
        }
        if (sessionContext != null) {
            return sessionContext.getServer();
        }
        return null;
    }

    private String resolveSourceTimestamp(JsonNode payload) {
        for (String field : new String[]{"time", "ts", "time_setup", "time_done"}) {
            if (payload.hasNonNull(field) && StringUtils.hasText(payload.path(field).asText())) {
                return payload.path(field).asText();
            }
        }
        return null;
    }

    private String resolveMasterAccountKey(Long login, String server, String sessionId) {
        if (login != null && StringUtils.hasText(server)) {
            return server + ":" + login;
        }
        return "session:" + sessionId;
    }

    private String resolveEventId(
            JsonNode payload,
            Mt5SignalType type,
            String masterAccountKey,
            String sessionId,
            String sourceTimestamp,
            Instant receivedAt
    ) {
        if (type.requiresEventId()) {
            String eventId = payload.path("event_id").asText(null);
            if (!StringUtils.hasText(eventId)) {
                throw new IllegalArgumentException("Signal event_id is required for " + type);
            }
            return eventId;
        }

        String timeKey = StringUtils.hasText(sourceTimestamp) ? sourceTimestamp : receivedAt.toString();
        if (type == Mt5SignalType.HELLO) {
            return "HELLO:" + masterAccountKey + ":" + timeKey;
        }
        return "HEARTBEAT:" + sessionId + ":" + timeKey;
    }
}
