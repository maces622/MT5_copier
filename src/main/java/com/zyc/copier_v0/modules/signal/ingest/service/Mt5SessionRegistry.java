package com.zyc.copier_v0.modules.signal.ingest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.monitor.config.WebSocketSessionRegistryBackend;
import com.zyc.copier_v0.modules.monitor.config.WebSocketSessionRegistryProperties;
import com.zyc.copier_v0.modules.signal.ingest.domain.Mt5SessionContext;
import java.util.Comparator;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class Mt5SessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(Mt5SessionRegistry.class);

    private final ConcurrentMap<String, Mt5SessionContext> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final WebSocketSessionRegistryProperties properties;

    public Mt5SessionRegistry(
            ObjectMapper objectMapper,
            StringRedisTemplate stringRedisTemplate,
            WebSocketSessionRegistryProperties properties
    ) {
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    public Mt5SessionContext register(String sessionId, String traceId) {
        Mt5SessionContext context = new Mt5SessionContext(sessionId, traceId, Instant.now(), null, null);
        sessions.put(sessionId, context);
        cacheSession(context);
        return context;
    }

    public Optional<Mt5SessionContext> get(String sessionId) {
        Mt5SessionContext local = sessions.get(sessionId);
        if (local != null) {
            return Optional.of(local);
        }
        if (!redisEnabled()) {
            return Optional.empty();
        }
        Optional<Mt5SessionContext> cached = readSession(sessionId);
        cached.ifPresent(context -> sessions.put(sessionId, context));
        return cached;
    }

    public void bindAccount(String sessionId, Long login, String server) {
        updateSession(sessionId, context -> context.withAccount(login, server));
    }

    public void touch(String sessionId) {
        updateSession(sessionId, context -> context);
    }

    public List<Mt5SessionContext> listAll() {
        if (!redisEnabled()) {
            return sortSessions(sessions.values().stream().toList());
        }

        List<Mt5SessionContext> cachedSessions = loadAllRedisSessions();
        if (!cachedSessions.isEmpty()) {
            cachedSessions.forEach(context -> sessions.put(context.getSessionId(), context));
            return sortSessions(cachedSessions);
        }

        return sortSessions(sessions.values().stream().toList());
    }

    public Optional<Mt5SessionContext> remove(String sessionId) {
        Mt5SessionContext removed = sessions.remove(sessionId);
        if (!redisEnabled()) {
            return Optional.ofNullable(removed);
        }

        Mt5SessionContext cached = removed != null ? removed : readSession(sessionId).orElse(null);
        removeCachedSession(sessionId);
        return Optional.ofNullable(cached);
    }

    private void updateSession(String sessionId, java.util.function.UnaryOperator<Mt5SessionContext> updater) {
        get(sessionId).ifPresent(context -> {
            Mt5SessionContext updated = updater.apply(context);
            sessions.put(sessionId, updated);
            cacheSession(updated);
        });
    }

    private List<Mt5SessionContext> loadAllRedisSessions() {
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
            log.warn("Failed to load MT5 websocket sessions from redis, fallback=memory", ex);
            return List.of();
        }
    }

    private Optional<Mt5SessionContext> readSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return Optional.empty();
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(sessionKey(sessionId));
            if (!StringUtils.hasText(json)) {
                cleanupIndexEntry(sessionId);
                return Optional.empty();
            }
            Mt5SessionCacheSnapshot snapshot = objectMapper.readValue(json, Mt5SessionCacheSnapshot.class);
            return Optional.of(toContext(snapshot));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize MT5 websocket session from redis, sessionId={}", sessionId, ex);
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Failed to read MT5 websocket session from redis, sessionId={}, fallback=memory", sessionId, ex);
            return Optional.empty();
        }
    }

    private void cacheSession(Mt5SessionContext context) {
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
            throw new IllegalStateException("Failed to serialize MT5 websocket session cache snapshot", ex);
        } catch (RuntimeException ex) {
            log.warn("Failed to cache MT5 websocket session in redis, sessionId={}, fallback=memory",
                    context.getSessionId(), ex);
        }
    }

    private void removeCachedSession(String sessionId) {
        try {
            stringRedisTemplate.delete(sessionKey(sessionId));
            cleanupIndexEntry(sessionId);
        } catch (RuntimeException ex) {
            log.warn("Failed to remove MT5 websocket session from redis, sessionId={}", sessionId, ex);
        }
    }

    private void cleanupIndexEntry(String sessionId) {
        try {
            stringRedisTemplate.opsForSet().remove(indexKey(), sessionId);
        } catch (RuntimeException ex) {
            log.warn("Failed to cleanup MT5 websocket session index in redis, sessionId={}", sessionId, ex);
        }
    }

    private Mt5SessionCacheSnapshot toSnapshot(Mt5SessionContext context) {
        Mt5SessionCacheSnapshot snapshot = new Mt5SessionCacheSnapshot();
        snapshot.setSessionId(context.getSessionId());
        snapshot.setTraceId(context.getTraceId());
        snapshot.setConnectedAt(context.getConnectedAt());
        snapshot.setLogin(context.getLogin());
        snapshot.setServer(context.getServer());
        return snapshot;
    }

    private Mt5SessionContext toContext(Mt5SessionCacheSnapshot snapshot) {
        return new Mt5SessionContext(
                snapshot.getSessionId(),
                snapshot.getTraceId(),
                snapshot.getConnectedAt(),
                snapshot.getLogin(),
                snapshot.getServer()
        );
    }

    private List<Mt5SessionContext> sortSessions(List<Mt5SessionContext> contexts) {
        return contexts.stream()
                .sorted(Comparator.comparing(Mt5SessionContext::getConnectedAt).reversed())
                .toList();
    }

    private boolean redisEnabled() {
        return properties.getBackend() == WebSocketSessionRegistryBackend.REDIS;
    }

    private String sessionKey(String sessionId) {
        return properties.getKeyPrefix() + ":mt5:session:" + sessionId;
    }

    private String indexKey() {
        return properties.getKeyPrefix() + ":mt5:index";
    }
}
