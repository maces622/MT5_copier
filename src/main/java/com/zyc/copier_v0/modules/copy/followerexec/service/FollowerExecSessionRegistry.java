package com.zyc.copier_v0.modules.copy.followerexec.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.monitor.config.WebSocketSessionRegistryBackend;
import com.zyc.copier_v0.modules.monitor.config.WebSocketSessionRegistryProperties;
import com.zyc.copier_v0.modules.copy.followerexec.domain.FollowerExecSessionContext;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FollowerExecSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(FollowerExecSessionRegistry.class);

    private final ConcurrentMap<String, FollowerExecSessionContext> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, String> sessionIdByFollowerAccountId = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final WebSocketSessionRegistryProperties properties;

    public FollowerExecSessionRegistry(
            ObjectMapper objectMapper,
            StringRedisTemplate stringRedisTemplate,
            WebSocketSessionRegistryProperties properties
    ) {
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

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
        cacheSession(context);
        return context;
    }

    public Optional<FollowerExecSessionContext> get(String sessionId) {
        FollowerExecSessionContext local = sessions.get(sessionId);
        if (local != null) {
            return Optional.of(local);
        }
        if (!redisEnabled()) {
            return Optional.empty();
        }
        Optional<FollowerExecSessionContext> cached = readSession(sessionId);
        cached.ifPresent(context -> {
            sessions.put(sessionId, context);
            if (context.getFollowerAccountId() != null) {
                sessionIdByFollowerAccountId.put(context.getFollowerAccountId(), context.getSessionId());
            }
        });
        return cached;
    }

    public void bindFollower(String sessionId, Long followerAccountId, Long login, String server, Instant helloAt) {
        updateSession(sessionId, context -> context.withFollower(followerAccountId, login, server, helloAt));
        if (followerAccountId != null) {
            sessionIdByFollowerAccountId.put(followerAccountId, sessionId);
        }
    }

    public void touchHeartbeat(String sessionId, Instant heartbeatAt) {
        updateSession(sessionId, context -> context.withHeartbeat(heartbeatAt));
    }

    public void recordDispatchSent(String sessionId, Long dispatchId, Instant sentAt) {
        updateSession(sessionId, context -> context.withDispatch(sentAt, dispatchId));
    }

    public Optional<String> findSessionIdByFollowerAccountId(Long followerAccountId) {
        String local = sessionIdByFollowerAccountId.get(followerAccountId);
        if (local != null) {
            return Optional.of(local);
        }
        if (!redisEnabled() || followerAccountId == null) {
            return Optional.empty();
        }
        try {
            String sessionId = stringRedisTemplate.opsForValue().get(bindingKey(followerAccountId));
            if (!StringUtils.hasText(sessionId)) {
                return Optional.empty();
            }
            return readSession(sessionId)
                    .map(context -> {
                        sessionIdByFollowerAccountId.put(followerAccountId, context.getSessionId());
                        sessions.put(context.getSessionId(), context);
                        return context.getSessionId();
                    });
        } catch (RuntimeException ex) {
            log.warn("Failed to read follower websocket binding from redis, followerAccountId={}, fallback=memory",
                    followerAccountId, ex);
            return Optional.empty();
        }
    }

    public List<FollowerExecSessionContext> listAll() {
        if (!redisEnabled()) {
            return sortSessions(sessions.values().stream().toList());
        }

        List<FollowerExecSessionContext> cachedSessions = loadAllRedisSessions();
        if (!cachedSessions.isEmpty()) {
            cachedSessions.forEach(context -> {
                sessions.put(context.getSessionId(), context);
                if (context.getFollowerAccountId() != null) {
                    sessionIdByFollowerAccountId.put(context.getFollowerAccountId(), context.getSessionId());
                }
            });
            return sortSessions(cachedSessions);
        }

        return sortSessions(sessions.values().stream().toList());
    }

    public Optional<FollowerExecSessionContext> remove(String sessionId) {
        FollowerExecSessionContext removed = sessions.remove(sessionId);
        if (removed != null && removed.getFollowerAccountId() != null) {
            sessionIdByFollowerAccountId.computeIfPresent(removed.getFollowerAccountId(), (key, currentSessionId) ->
                    sessionId.equals(currentSessionId) ? null : currentSessionId
            );
        }
        if (!redisEnabled()) {
            return Optional.ofNullable(removed);
        }

        FollowerExecSessionContext cached = removed != null ? removed : readSession(sessionId).orElse(null);
        removeCachedSession(sessionId, cached != null ? cached.getFollowerAccountId() : null);
        return Optional.ofNullable(cached);
    }

    private void updateSession(
            String sessionId,
            java.util.function.UnaryOperator<FollowerExecSessionContext> updater
    ) {
        get(sessionId).ifPresent(context -> {
            FollowerExecSessionContext updated = updater.apply(context);
            sessions.put(sessionId, updated);
            if (updated.getFollowerAccountId() != null) {
                sessionIdByFollowerAccountId.put(updated.getFollowerAccountId(), updated.getSessionId());
                cacheFollowerBinding(updated.getFollowerAccountId(), updated.getSessionId());
            }
            cacheSession(updated);
        });
    }

    private List<FollowerExecSessionContext> loadAllRedisSessions() {
        try {
            SetOperations<String, String> setOperations = stringRedisTemplate.opsForSet();
            Set<String> sessionIds = setOperations.members(indexKey());
            if (sessionIds == null || sessionIds.isEmpty()) {
                return List.of();
            }
            return sessionIds.stream()
                    .map(this::readSession)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("Failed to load follower websocket sessions from redis, fallback=memory", ex);
            return List.of();
        }
    }

    private Optional<FollowerExecSessionContext> readSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return Optional.empty();
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(sessionKey(sessionId));
            if (!StringUtils.hasText(json)) {
                cleanupIndexEntry(sessionId);
                return Optional.empty();
            }
            FollowerExecSessionCacheSnapshot snapshot = objectMapper.readValue(json, FollowerExecSessionCacheSnapshot.class);
            return Optional.of(toContext(snapshot));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize follower websocket session from redis, sessionId={}", sessionId, ex);
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Failed to read follower websocket session from redis, sessionId={}, fallback=memory",
                    sessionId, ex);
            return Optional.empty();
        }
    }

    private void cacheSession(FollowerExecSessionContext context) {
        if (!redisEnabled() || context == null || !StringUtils.hasText(context.getSessionId())) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    sessionKey(context.getSessionId()),
                    objectMapper.writeValueAsString(toSnapshot(context)),
                    properties.getTtl()
            );
            stringRedisTemplate.opsForSet().add(indexKey(), context.getSessionId());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize follower websocket session cache snapshot", ex);
        } catch (RuntimeException ex) {
            log.warn("Failed to cache follower websocket session in redis, sessionId={}, fallback=memory",
                    context.getSessionId(), ex);
        }
    }

    private void cacheFollowerBinding(Long followerAccountId, String sessionId) {
        if (!redisEnabled() || followerAccountId == null || !StringUtils.hasText(sessionId)) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(bindingKey(followerAccountId), sessionId, properties.getTtl());
        } catch (RuntimeException ex) {
            log.warn("Failed to cache follower websocket binding in redis, followerAccountId={}", followerAccountId, ex);
        }
    }

    private void removeCachedSession(String sessionId, Long followerAccountId) {
        try {
            stringRedisTemplate.delete(sessionKey(sessionId));
            cleanupIndexEntry(sessionId);
            if (followerAccountId != null) {
                String current = stringRedisTemplate.opsForValue().get(bindingKey(followerAccountId));
                if (sessionId.equals(current)) {
                    stringRedisTemplate.delete(bindingKey(followerAccountId));
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to remove follower websocket session from redis, sessionId={}", sessionId, ex);
        }
    }

    private void cleanupIndexEntry(String sessionId) {
        try {
            stringRedisTemplate.opsForSet().remove(indexKey(), sessionId);
        } catch (RuntimeException ex) {
            log.warn("Failed to cleanup follower websocket session index in redis, sessionId={}", sessionId, ex);
        }
    }

    private FollowerExecSessionCacheSnapshot toSnapshot(FollowerExecSessionContext context) {
        FollowerExecSessionCacheSnapshot snapshot = new FollowerExecSessionCacheSnapshot();
        snapshot.setSessionId(context.getSessionId());
        snapshot.setTraceId(context.getTraceId());
        snapshot.setConnectedAt(context.getConnectedAt());
        snapshot.setFollowerAccountId(context.getFollowerAccountId());
        snapshot.setLogin(context.getLogin());
        snapshot.setServer(context.getServer());
        snapshot.setLastHelloAt(context.getLastHelloAt());
        snapshot.setLastHeartbeatAt(context.getLastHeartbeatAt());
        snapshot.setLastDispatchSentAt(context.getLastDispatchSentAt());
        snapshot.setLastDispatchId(context.getLastDispatchId());
        return snapshot;
    }

    private FollowerExecSessionContext toContext(FollowerExecSessionCacheSnapshot snapshot) {
        return new FollowerExecSessionContext(
                snapshot.getSessionId(),
                snapshot.getTraceId(),
                snapshot.getConnectedAt(),
                snapshot.getFollowerAccountId(),
                snapshot.getLogin(),
                snapshot.getServer(),
                snapshot.getLastHelloAt(),
                snapshot.getLastHeartbeatAt(),
                snapshot.getLastDispatchSentAt(),
                snapshot.getLastDispatchId()
        );
    }

    private List<FollowerExecSessionContext> sortSessions(List<FollowerExecSessionContext> contexts) {
        return contexts.stream()
                .sorted(Comparator.comparing(FollowerExecSessionContext::getConnectedAt).reversed())
                .toList();
    }

    private boolean redisEnabled() {
        return properties.getBackend() == WebSocketSessionRegistryBackend.REDIS;
    }

    private String sessionKey(String sessionId) {
        return properties.getKeyPrefix() + ":follower:session:" + sessionId;
    }

    private String bindingKey(Long followerAccountId) {
        return properties.getKeyPrefix() + ":follower:account:" + followerAccountId;
    }

    private String indexKey() {
        return properties.getKeyPrefix() + ":follower:index";
    }
}
