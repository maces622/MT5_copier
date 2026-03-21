package com.zyc.copier_v0.modules.copy.followerexec.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.copy.followerexec.domain.FollowerExecSessionContext;
import com.zyc.copier_v0.modules.monitor.config.WebSocketSessionRegistryBackend;
import com.zyc.copier_v0.modules.monitor.config.WebSocketSessionRegistryProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class FollowerExecSessionRegistryTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private FollowerExecSessionRegistry registry;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        WebSocketSessionRegistryProperties properties = new WebSocketSessionRegistryProperties();
        properties.setBackend(WebSocketSessionRegistryBackend.REDIS);
        properties.setKeyPrefix("copy:ws");
        properties.setTtl(Duration.ofMinutes(2));
        registry = new FollowerExecSessionRegistry(objectMapper, stringRedisTemplate, properties);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldCacheFollowerBindingInRedisOnHello() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        registry.register("session-1", "trace-1");
        registry.bindFollower("session-1", 2L, 51629L, "Broker-Demo", Instant.parse("2026-03-21T10:00:00Z"));

        assertThat(registry.findSessionIdByFollowerAccountId(2L)).contains("session-1");
        verify(valueOperations).set(
                eq("copy:ws:follower:session:session-1"),
                contains("\"followerAccountId\":2"),
                eq(Duration.ofMinutes(2))
        );
        verify(valueOperations).set("copy:ws:follower:account:2", "session-1", Duration.ofMinutes(2));
    }

    @Test
    void shouldLoadFollowerBindingFromRedisWhenLocalCacheIsEmpty() throws Exception {
        FollowerExecSessionCacheSnapshot snapshot = new FollowerExecSessionCacheSnapshot();
        snapshot.setSessionId("session-2");
        snapshot.setTraceId("trace-2");
        snapshot.setConnectedAt(Instant.parse("2026-03-21T10:05:00Z"));
        snapshot.setFollowerAccountId(3L);
        snapshot.setLogin(60001L);
        snapshot.setServer("Broker-Live");

        when(valueOperations.get("copy:ws:follower:account:3")).thenReturn("session-2");
        when(valueOperations.get("copy:ws:follower:session:session-2"))
                .thenReturn(objectMapper.writeValueAsString(snapshot));

        Optional<String> sessionId = registry.findSessionIdByFollowerAccountId(3L);
        Optional<FollowerExecSessionContext> context = registry.get("session-2");

        assertThat(sessionId).contains("session-2");
        assertThat(context).isPresent();
        assertThat(context.orElseThrow().accountKey()).isEqualTo("Broker-Live:60001");
    }
}
