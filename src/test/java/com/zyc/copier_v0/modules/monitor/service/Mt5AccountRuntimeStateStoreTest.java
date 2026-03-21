package com.zyc.copier_v0.modules.monitor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zyc.copier_v0.modules.monitor.config.Mt5RuntimeStateBackend;
import com.zyc.copier_v0.modules.monitor.config.Mt5RuntimeStateProperties;
import com.zyc.copier_v0.modules.monitor.domain.Mt5ConnectionStatus;
import com.zyc.copier_v0.modules.monitor.entity.Mt5AccountRuntimeStateEntity;
import com.zyc.copier_v0.modules.monitor.repository.Mt5AccountRuntimeStateRepository;
import java.math.BigDecimal;
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
class Mt5AccountRuntimeStateStoreTest {

    @Mock
    private Mt5AccountRuntimeStateRepository repository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Mt5AccountRuntimeStateStore store;
    private Mt5RuntimeStateProperties properties;

    @BeforeEach
    void setUp() {
        properties = new Mt5RuntimeStateProperties();
        properties.setBackend(Mt5RuntimeStateBackend.REDIS);
        properties.setKeyPrefix("copy:runtime");
        properties.setDatabaseSyncInterval(Duration.ofSeconds(30));
        properties.setFundsStaleAfter(Duration.ofSeconds(30));
        properties.setRequireFreshFundsForRatio(true);

        store = new Mt5AccountRuntimeStateStore(repository, stringRedisTemplate, objectMapper, properties);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void shouldLoadRuntimeStateFromRedisByAccountIdWithoutDatabaseFallback() throws Exception {
        Mt5AccountRuntimeStateSnapshot cached = snapshot(2L, 51629L, "Broker-Live");
        when(valueOperations.get("copy:runtime:account:2")).thenReturn("Broker-Live:51629");
        when(valueOperations.get("copy:runtime:state:Broker-Live:51629"))
                .thenReturn(objectMapper.writeValueAsString(cached));

        Optional<Mt5AccountRuntimeStateSnapshot> loaded = store.findByAccountId(2L);

        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().getBalance()).isEqualByComparingTo("5000");
        verify(repository, never()).findByAccountId(2L);
    }

    @Test
    void shouldFallbackToDatabaseAndBackfillRedisOnAccountLookupMiss() {
        Mt5AccountRuntimeStateEntity entity = entity(2L, 51629L, "Broker-Live");
        when(valueOperations.get("copy:runtime:account:2")).thenReturn(null);
        when(repository.findByAccountId(2L)).thenReturn(Optional.of(entity));

        Optional<Mt5AccountRuntimeStateSnapshot> loaded = store.findByAccountId(2L);

        assertThat(loaded).isPresent();
        verify(repository).findByAccountId(2L);
        verify(valueOperations).set(eq("copy:runtime:state:Broker-Live:51629"), contains("\"accountId\":2"));
        verify(setOperations).add("copy:runtime:index", "Broker-Live:51629");
        verify(valueOperations).set("copy:runtime:account:2", "Broker-Live:51629");
    }

    @Test
    void shouldThrottleDatabaseSyncForRedisBackend() {
        Mt5AccountRuntimeStateSnapshot snapshot = snapshot(2L, 51629L, "Broker-Live");
        when(valueOperations.setIfAbsent(eq("copy:runtime:db-sync:Broker-Live:51629"), eq("1"), eq(Duration.ofSeconds(30))))
                .thenReturn(true, false);
        when(repository.findByServerAndLogin("Broker-Live", 51629L)).thenReturn(Optional.empty());
        when(repository.save(any(Mt5AccountRuntimeStateEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        store.maybePersist(snapshot);
        store.maybePersist(snapshot);

        verify(repository).save(any(Mt5AccountRuntimeStateEntity.class));
    }

    @Test
    void shouldRejectStaleRuntimeStateForFundsLookup() {
        Mt5AccountRuntimeStateEntity entity = entity(2L, 51629L, "Broker-Live");
        entity.setLastHeartbeatAt(Instant.now().minus(Duration.ofMinutes(2)));
        when(valueOperations.get("copy:runtime:account:2")).thenReturn(null);
        when(repository.findByAccountId(2L)).thenReturn(Optional.of(entity));

        Optional<Mt5AccountRuntimeStateSnapshot> loaded = store.findFreshByAccountId(2L);

        assertThat(loaded).isEmpty();
    }

    @Test
    void shouldAllowStaleRuntimeStateWhenFreshFundsGateIsDisabled() {
        properties.setRequireFreshFundsForRatio(false);
        Mt5AccountRuntimeStateEntity entity = entity(2L, 51629L, "Broker-Live");
        entity.setLastHeartbeatAt(Instant.now().minus(Duration.ofMinutes(2)));
        when(valueOperations.get("copy:runtime:account:2")).thenReturn(null);
        when(repository.findByAccountId(2L)).thenReturn(Optional.of(entity));

        Optional<Mt5AccountRuntimeStateSnapshot> loaded = store.findFreshByAccountId(2L);

        assertThat(loaded).isPresent();
    }

    private Mt5AccountRuntimeStateSnapshot snapshot(Long accountId, Long login, String server) {
        Mt5AccountRuntimeStateSnapshot snapshot = new Mt5AccountRuntimeStateSnapshot();
        snapshot.setAccountId(accountId);
        snapshot.setLogin(login);
        snapshot.setServer(server);
        snapshot.setAccountKey(server + ":" + login);
        snapshot.setConnectionStatus(Mt5ConnectionStatus.CONNECTED);
        snapshot.setBalance(new BigDecimal("5000"));
        snapshot.setEquity(new BigDecimal("4900"));
        snapshot.setLastHeartbeatAt(Instant.parse("2026-03-21T11:00:00Z"));
        snapshot.setUpdatedAt(Instant.parse("2026-03-21T11:00:00Z"));
        return snapshot;
    }

    private Mt5AccountRuntimeStateEntity entity(Long accountId, Long login, String server) {
        Mt5AccountRuntimeStateEntity entity = new Mt5AccountRuntimeStateEntity();
        entity.setAccountId(accountId);
        entity.setLogin(login);
        entity.setServer(server);
        entity.setAccountKey(server + ":" + login);
        entity.setConnectionStatus(Mt5ConnectionStatus.CONNECTED);
        entity.setBalance(new BigDecimal("5000"));
        entity.setEquity(new BigDecimal("4900"));
        entity.setLastHeartbeatAt(Instant.now());
        return entity;
    }
}
