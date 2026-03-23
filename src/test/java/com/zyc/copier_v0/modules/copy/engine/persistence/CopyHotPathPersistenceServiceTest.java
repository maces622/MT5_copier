package com.zyc.copier_v0.modules.copy.engine.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zyc.copier_v0.modules.copy.engine.domain.FollowerDispatchStatus;
import com.zyc.copier_v0.modules.copy.engine.entity.FollowerDispatchOutboxEntity;
import com.zyc.copier_v0.modules.copy.engine.repository.ExecutionCommandRepository;
import com.zyc.copier_v0.modules.copy.engine.repository.FollowerDispatchOutboxRepository;
import com.zyc.copier_v0.modules.monitor.repository.Mt5SignalRecordRepository;
import com.zyc.copier_v0.modules.monitor.service.Mt5PositionLedgerPersistenceService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CopyHotPathPersistenceServiceTest {

    @Mock
    private Mt5SignalRecordRepository signalRecordRepository;

    @Mock
    private ExecutionCommandRepository executionCommandRepository;

    @Mock
    private FollowerDispatchOutboxRepository followerDispatchOutboxRepository;

    @Mock
    private Mt5PositionLedgerPersistenceService positionLedgerPersistenceService;

    private CopyHotPathPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new CopyHotPathPersistenceService(
                new ObjectMapper().registerModule(new JavaTimeModule()),
                signalRecordRepository,
                executionCommandRepository,
                followerDispatchOutboxRepository,
                positionLedgerPersistenceService
        );
    }

    @Test
    void upsertFollowerDispatchShouldReuseExistingRowMatchedByExecutionCommandId() {
        FollowerDispatchOutboxEntity existing = new FollowerDispatchOutboxEntity();
        existing.setId(7L);
        existing.setExecutionCommandId(20L);
        existing.setFollowerAccountId(2L);
        existing.setMasterEventId("old-event");
        existing.setStatus(FollowerDispatchStatus.PENDING);

        FollowerDispatchOutboxEntity snapshot = new FollowerDispatchOutboxEntity();
        snapshot.setId(10L);
        snapshot.setExecutionCommandId(20L);
        snapshot.setFollowerAccountId(2L);
        snapshot.setMasterEventId("new-event");
        snapshot.setStatus(FollowerDispatchStatus.ACKED);
        snapshot.setStatusMessage("acked");
        snapshot.setPayloadJson("{\"dispatchId\":10}");
        snapshot.setAckedAt(Instant.parse("2026-03-23T03:05:00Z"));

        when(followerDispatchOutboxRepository.findById(10L)).thenReturn(Optional.empty());
        when(followerDispatchOutboxRepository.findByExecutionCommandId(20L)).thenReturn(Optional.of(existing));

        persistenceService.upsertFollowerDispatch(snapshot);

        ArgumentCaptor<FollowerDispatchOutboxEntity> captor = ArgumentCaptor.forClass(FollowerDispatchOutboxEntity.class);
        verify(followerDispatchOutboxRepository).save(captor.capture());
        FollowerDispatchOutboxEntity saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(7L);
        assertThat(saved.getExecutionCommandId()).isEqualTo(20L);
        assertThat(saved.getMasterEventId()).isEqualTo("new-event");
        assertThat(saved.getStatus()).isEqualTo(FollowerDispatchStatus.ACKED);
        assertThat(saved.getStatusMessage()).isEqualTo("acked");
        assertThat(saved.getPayloadJson()).isEqualTo("{\"dispatchId\":10}");
        assertThat(saved.getAckedAt()).isEqualTo(Instant.parse("2026-03-23T03:05:00Z"));
    }
}
