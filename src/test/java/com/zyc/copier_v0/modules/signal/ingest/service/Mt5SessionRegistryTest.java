package com.zyc.copier_v0.modules.signal.ingest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.monitor.config.WebSocketSessionRegistryBackend;
import com.zyc.copier_v0.modules.monitor.config.WebSocketSessionRegistryProperties;
import com.zyc.copier_v0.modules.signal.ingest.domain.Mt5SessionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class Mt5SessionRegistryTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private Mt5SessionRegistry registry;
    private WebSocketSessionRegistryProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        properties = new WebSocketSessionRegistryProperties();
        properties.setBackend(WebSocketSessionRegistryBackend.REDIS);
        properties.setKeyPrefix("copy:ws");
        properties.setTtl(Duration.ofMinutes(2));
        registry = new Mt5SessionRegistry(objectMapper, stringRedisTemplate, properties);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldCacheSessionInRedisOnRegisterAndBind() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        registry.register("session-1", "trace-1");
        registry.bindAccount("session-1", 123456L, "Broker-Demo");

        Optional<Mt5SessionContext> loaded = registry.get("session-1");

        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().getLogin()).isEqualTo(123456L);
        verify(valueOperations, times(2)).set(
                eq("copy:ws:mt5:session:session-1"),
                contains("\"sessionId\":\"session-1\""),
                eq(Duration.ofMinutes(2))
        );
        verify(setOperations, times(2)).add("copy:ws:mt5:index", "session-1");
    }

    @Test
    void shouldLoadSessionFromRedisWhenLocalCacheIsEmpty() throws Exception {
        Mt5SessionCacheSnapshot snapshot = new Mt5SessionCacheSnapshot();
        snapshot.setSessionId("session-2");
        snapshot.setTraceId("trace-2");
        snapshot.setConnectedAt(Instant.parse("2026-03-21T10:00:00Z"));
        snapshot.setLogin(654321L);
        snapshot.setServer("Broker-Live");
        when(valueOperations.get("copy:ws:mt5:session:session-2"))
                .thenReturn(objectMapper.writeValueAsString(snapshot));

        Optional<Mt5SessionContext> loaded = registry.get("session-2");

        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().masterAccountKey()).isEqualTo("Broker-Live:654321");
    }

    @Test
    void shouldListSessionsFromRedisIndex() throws Exception {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        Mt5SessionCacheSnapshot snapshot = new Mt5SessionCacheSnapshot();
        snapshot.setSessionId("session-3");
        snapshot.setTraceId("trace-3");
        snapshot.setConnectedAt(Instant.parse("2026-03-21T10:10:00Z"));
        when(setOperations.members("copy:ws:mt5:index")).thenReturn(Set.of("session-3"));
        when(valueOperations.get("copy:ws:mt5:session:session-3"))
                .thenReturn(objectMapper.writeValueAsString(snapshot));

        assertThat(registry.listAll()).extracting(Mt5SessionContext::getSessionId).containsExactly("session-3");
    }
}
