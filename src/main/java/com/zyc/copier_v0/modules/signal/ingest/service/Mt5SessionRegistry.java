package com.zyc.copier_v0.modules.signal.ingest.service;

import com.zyc.copier_v0.modules.signal.ingest.domain.Mt5SessionContext;
import java.util.Comparator;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class Mt5SessionRegistry {

    private final ConcurrentMap<String, Mt5SessionContext> sessions = new ConcurrentHashMap<>();

    public Mt5SessionContext register(String sessionId, String traceId) {
        Mt5SessionContext context = new Mt5SessionContext(sessionId, traceId, Instant.now(), null, null);
        sessions.put(sessionId, context);
        return context;
    }

    public Optional<Mt5SessionContext> get(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public void bindAccount(String sessionId, Long login, String server) {
        sessions.computeIfPresent(sessionId, (key, context) -> context.withAccount(login, server));
    }

    public List<Mt5SessionContext> listAll() {
        return sessions.values().stream()
                .sorted(Comparator.comparing(Mt5SessionContext::getConnectedAt).reversed())
                .toList();
    }

    public Optional<Mt5SessionContext> remove(String sessionId) {
        return Optional.ofNullable(sessions.remove(sessionId));
    }
}
