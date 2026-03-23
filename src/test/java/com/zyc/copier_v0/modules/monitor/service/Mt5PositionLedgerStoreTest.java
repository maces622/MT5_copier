package com.zyc.copier_v0.modules.monitor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zyc.copier_v0.modules.copy.engine.persistence.CopyHotPathPersistenceQueue;
import com.zyc.copier_v0.modules.monitor.config.Mt5RuntimeStateProperties;
import com.zyc.copier_v0.modules.monitor.entity.Mt5OpenPositionEntity;
import com.zyc.copier_v0.modules.monitor.repository.Mt5OpenPositionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class Mt5PositionLedgerStoreTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private Mt5OpenPositionRepository repository;

    @Mock
    private CopyHotPathPersistenceQueue persistenceQueue;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Mt5PositionLedgerStore store;

    @BeforeEach
    void setUp() {
        Mt5RuntimeStateProperties properties = new Mt5RuntimeStateProperties();
        properties.setKeyPrefix("copy:runtime");
        properties.setWarmupOnStartup(true);

        store = new Mt5PositionLedgerStore(
                properties,
                stringRedisTemplate,
                objectMapper,
                repository,
                persistenceQueue
        );
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void shouldExtractTrackedHeldPositionsFromPayload() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "positions": [
                    {
                      "ticket": 90001,
                      "position": 35954039,
                      "order": 0,
                      "symbol": "BTCUSD",
                      "volume": 0.13,
                      "price_open": 70628.41,
                      "sl": 70363.82,
                      "tp": 70848.98,
                      "comment": "cp1|mp=35954038|mo=35954030"
                    }
                  ]
                }
                """);

        List<Mt5OpenPositionSnapshot> positions = store.extractFromPayload(
                payload,
                Instant.parse("2026-03-23T10:00:00Z")
        );

        assertThat(positions).hasSize(1);
        Mt5OpenPositionSnapshot snapshot = positions.get(0);
        assertThat(snapshot.getPositionKey()).isEqualTo("mp:35954038");
        assertThat(snapshot.getMasterPositionId()).isEqualTo(35954038L);
        assertThat(snapshot.getMasterOrderId()).isEqualTo(35954030L);
        assertThat(snapshot.getSourcePositionId()).isEqualTo(35954039L);
        assertThat(snapshot.getVolume()).isEqualByComparingTo("0.13");
        assertThat(snapshot.getCommentText()).isEqualTo("cp1|mp=35954038|mo=35954030");
    }

    @Test
    void shouldBackfillRedisFromDatabaseOnLedgerMiss() {
        Mt5OpenPositionEntity entity = new Mt5OpenPositionEntity();
        entity.setAccountId(2L);
        entity.setLogin(51629L);
        entity.setServer("Broker-Live");
        entity.setAccountKey("Broker-Live:51629");
        entity.setPositionKey("mp:35954038");
        entity.setSourcePositionId(35954039L);
        entity.setMasterPositionId(35954038L);
        entity.setMasterOrderId(35954030L);
        entity.setSymbol("BTCUSD");
        entity.setVolume(new BigDecimal("0.13"));
        entity.setPriceOpen(new BigDecimal("70628.41"));
        entity.setSl(new BigDecimal("70363.82"));
        entity.setTp(new BigDecimal("70848.98"));
        entity.setCommentText("cp1|mp=35954038|mo=35954030");
        entity.setObservedAt(Instant.parse("2026-03-23T10:00:00Z"));

        when(valueOperations.get("copy:runtime:positions:Broker-Live:51629")).thenReturn(null);
        when(repository.findByAccountKeyOrderByPositionKeyAsc("Broker-Live:51629")).thenReturn(List.of(entity));

        List<Mt5OpenPositionSnapshot> loaded = store.findByAccountKey("Broker-Live:51629");

        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getPositionKey()).isEqualTo("mp:35954038");
        verify(valueOperations).set(eq("copy:runtime:positions:Broker-Live:51629"), contains("\"positionKey\":\"mp:35954038\""));
        verify(setOperations).add("copy:runtime:positions:index", "Broker-Live:51629");
    }

    @Test
    void shouldEnqueuePersistenceWhenLedgerSnapshotChanges() throws Exception {
        Instant observedAt = Instant.parse("2026-03-23T10:00:00Z");
        Mt5OpenPositionSnapshot snapshot = snapshot(observedAt);
        when(valueOperations.get("copy:runtime:positions:Broker-Live:51629")).thenReturn(null);
        when(repository.findByAccountKeyOrderByPositionKeyAsc("Broker-Live:51629")).thenReturn(List.of());

        store.reconcile(
                2L,
                51629L,
                "Broker-Live",
                "Broker-Live:51629",
                List.of(snapshot),
                observedAt
        );

        verify(valueOperations).set(eq("copy:runtime:positions:Broker-Live:51629"), contains("\"positionKey\":\"mp:35954038\""));
        ArgumentCaptor<Mt5PositionLedgerReconcileMessage> captor = ArgumentCaptor.forClass(Mt5PositionLedgerReconcileMessage.class);
        verify(persistenceQueue).enqueuePositionLedger(captor.capture());
        assertThat(captor.getValue().getAccountId()).isEqualTo(2L);
        assertThat(captor.getValue().getPositions()).hasSize(1);
        assertThat(captor.getValue().getPositions().get(0).getPositionKey()).isEqualTo("mp:35954038");
    }

    @Test
    void shouldSkipPersistenceWhenLedgerSnapshotHasNotChanged() throws Exception {
        Instant observedAt = Instant.parse("2026-03-23T10:00:00Z");
        Mt5OpenPositionSnapshot snapshot = snapshot(observedAt);
        when(valueOperations.get("copy:runtime:positions:Broker-Live:51629"))
                .thenReturn(objectMapper.writeValueAsString(List.of(snapshot)));

        store.reconcile(
                2L,
                51629L,
                "Broker-Live",
                "Broker-Live:51629",
                List.of(snapshot(observedAt)),
                observedAt
        );

        verify(persistenceQueue, never()).enqueuePositionLedger(any());
    }

    private Mt5OpenPositionSnapshot snapshot(Instant observedAt) {
        Mt5OpenPositionSnapshot snapshot = new Mt5OpenPositionSnapshot();
        snapshot.setPositionKey("mp:35954038");
        snapshot.setSourcePositionId(35954039L);
        snapshot.setMasterPositionId(35954038L);
        snapshot.setMasterOrderId(35954030L);
        snapshot.setSymbol("BTCUSD");
        snapshot.setVolume(new BigDecimal("0.13"));
        snapshot.setPriceOpen(new BigDecimal("70628.41"));
        snapshot.setSl(new BigDecimal("70363.82"));
        snapshot.setTp(new BigDecimal("70848.98"));
        snapshot.setCommentText("cp1|mp=35954038|mo=35954030");
        snapshot.setObservedAt(observedAt);
        return snapshot;
    }
}
