package com.zyc.copier_v0.modules.monitor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.account.config.cache.CopyRouteSnapshotReader;
import com.zyc.copier_v0.modules.account.config.repository.CopyRelationRepository;
import com.zyc.copier_v0.modules.account.config.repository.Mt5AccountRepository;
import com.zyc.copier_v0.modules.copy.engine.persistence.CopyHotPathIdAllocator;
import com.zyc.copier_v0.modules.copy.engine.persistence.CopyHotPathPersistenceQueue;
import com.zyc.copier_v0.modules.copy.engine.repository.FollowerDispatchOutboxRepository;
import com.zyc.copier_v0.modules.monitor.api.Mt5RuntimeStateResponse;
import com.zyc.copier_v0.modules.monitor.config.MonitorProperties;
import com.zyc.copier_v0.modules.monitor.domain.Mt5ConnectionStatus;
import com.zyc.copier_v0.modules.monitor.repository.Mt5SignalRecordRepository;
import com.zyc.copier_v0.modules.signal.ingest.service.Mt5SessionRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountMonitorServiceTest {

    @Mock
    private Mt5SignalRecordRepository signalRecordRepository;

    @Mock
    private Mt5AccountRuntimeStateStore runtimeStateStore;

    @Mock
    private Mt5AccountRepository mt5AccountRepository;

    @Mock
    private CopyRelationRepository copyRelationRepository;

    @Mock
    private FollowerDispatchOutboxRepository followerDispatchOutboxRepository;

    @Mock
    private Mt5SessionRegistry mt5SessionRegistry;

    @Mock
    private CopyRouteSnapshotReader copyRouteSnapshotReader;

    @Mock
    private CopyHotPathIdAllocator hotPathIdAllocator;

    @Mock
    private CopyHotPathPersistenceQueue hotPathPersistenceQueue;

    @Mock
    private Mt5PositionLedgerStore positionLedgerStore;

    private AccountMonitorService accountMonitorService;

    @BeforeEach
    void setUp() {
        MonitorProperties monitorProperties = new MonitorProperties();
        monitorProperties.setHeartbeatStaleAfter(Duration.ofMinutes(1));
        accountMonitorService = new AccountMonitorService(
                signalRecordRepository,
                runtimeStateStore,
                mt5AccountRepository,
                copyRelationRepository,
                followerDispatchOutboxRepository,
                mt5SessionRegistry,
                copyRouteSnapshotReader,
                new ObjectMapper(),
                monitorProperties,
                hotPathIdAllocator,
                hotPathPersistenceQueue,
                positionLedgerStore
        );
    }

    @Test
    void listRuntimeStatesShouldExposeBalanceAndEquity() {
        Mt5AccountRuntimeStateSnapshot snapshot = new Mt5AccountRuntimeStateSnapshot();
        snapshot.setId(9L);
        snapshot.setAccountId(7L);
        snapshot.setLogin(51813L);
        snapshot.setServer("EBCFinancialGroupKY-Demo");
        snapshot.setAccountKey("EBCFinancialGroupKY-Demo:51813");
        snapshot.setConnectionStatus(Mt5ConnectionStatus.CONNECTED);
        snapshot.setBalance(new BigDecimal("10234.56"));
        snapshot.setEquity(new BigDecimal("10001.23"));
        snapshot.setLastHelloAt(Instant.parse("2026-03-23T04:17:03Z"));
        snapshot.setLastHeartbeatAt(Instant.now());
        snapshot.setUpdatedAt(Instant.now());
        when(runtimeStateStore.listAll()).thenReturn(List.of(snapshot));

        List<Mt5RuntimeStateResponse> responses = accountMonitorService.listRuntimeStates();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBalance()).isEqualByComparingTo("10234.56");
        assertThat(responses.get(0).getEquity()).isEqualByComparingTo("10001.23");
    }
}
