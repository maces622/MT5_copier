package com.zyc.copier_v0.modules.copy.followerexec.service;

import com.zyc.copier_v0.modules.copy.followerexec.domain.FollowerExecSessionContext;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class FollowerExecSessionRegistry {

    private final ConcurrentMap<String, FollowerExecSessionContext> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, String> sessionIdByFollowerAccountId = new ConcurrentHashMap<>();

    public FollowerExecSessionContext register(String sessionId, String traceId) {
        FollowerExecSessionContext context = new FollowerExecSessionContext(
                sessionId,
                traceId,
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        sessions.put(sessionId, context);
        return context;
    }

    public Optional<FollowerExecSessionContext> get(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public void bindFollower(String sessionId, Long followerAccountId, Long login, String server, Instant helloAt) {
        sessions.computeIfPresent(sessionId, (key, context) -> context.withFollower(followerAccountId, login, server, helloAt));
        if (followerAccountId != null) {
            sessionIdByFollowerAccountId.put(followerAccountId, sessionId);
        }
    }

    public void touchHeartbeat(String sessionId, Instant heartbeatAt) {
        sessions.computeIfPresent(sessionId, (key, context) -> context.withHeartbeat(heartbeatAt));
    }

    public void recordDispatchSent(String sessionId, Long dispatchId, Instant sentAt) {
        sessions.computeIfPresent(sessionId, (key, context) -> context.withDispatch(sentAt, dispatchId));
    }

    public Optional<String> findSessionIdByFollowerAccountId(Long followerAccountId) {
        return Optional.ofNullable(sessionIdByFollowerAccountId.get(followerAccountId));
    }

    public List<FollowerExecSessionContext> listAll() {
        return sessions.values().stream()
                .sorted(Comparator.comparing(FollowerExecSessionContext::getConnectedAt).reversed())
                .toList();
    }

    public Optional<FollowerExecSessionContext> remove(String sessionId) {
        FollowerExecSessionContext removed = sessions.remove(sessionId);
        if (removed != null && removed.getFollowerAccountId() != null) {
            sessionIdByFollowerAccountId.computeIfPresent(removed.getFollowerAccountId(), (key, currentSessionId) ->
                    sessionId.equals(currentSessionId) ? null : currentSessionId
            );
        }
        return Optional.ofNullable(removed);
    }
}
