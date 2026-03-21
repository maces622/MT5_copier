package com.zyc.copier_v0.modules.monitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;
import com.zyc.copier_v0.modules.account.config.domain.Mt5AccountRole;
import com.zyc.copier_v0.modules.account.config.entity.CopyRelationEntity;
import com.zyc.copier_v0.modules.account.config.entity.Mt5AccountEntity;
import com.zyc.copier_v0.modules.account.config.repository.Mt5AccountRepository;
import com.zyc.copier_v0.modules.account.config.repository.CopyRelationRepository;
import com.zyc.copier_v0.modules.copy.engine.domain.FollowerDispatchStatus;
import com.zyc.copier_v0.modules.copy.engine.entity.FollowerDispatchOutboxEntity;
import com.zyc.copier_v0.modules.copy.engine.repository.FollowerDispatchOutboxRepository;
import com.zyc.copier_v0.modules.monitor.api.Mt5AccountMonitorOverviewResponse;
import com.zyc.copier_v0.modules.monitor.api.Mt5RuntimeStateResponse;
import com.zyc.copier_v0.modules.monitor.api.Mt5SignalRecordResponse;
import com.zyc.copier_v0.modules.monitor.api.Mt5WsSessionResponse;
import com.zyc.copier_v0.modules.monitor.config.MonitorProperties;
import com.zyc.copier_v0.modules.monitor.domain.Mt5ConnectionStatus;
import com.zyc.copier_v0.modules.monitor.entity.Mt5AccountRuntimeStateEntity;
import com.zyc.copier_v0.modules.monitor.entity.Mt5SignalRecordEntity;
import com.zyc.copier_v0.modules.monitor.repository.Mt5AccountRuntimeStateRepository;
import com.zyc.copier_v0.modules.monitor.repository.Mt5SignalRecordRepository;
import com.zyc.copier_v0.modules.signal.ingest.domain.Mt5SessionContext;
import com.zyc.copier_v0.modules.signal.ingest.domain.NormalizedMt5Signal;
import com.zyc.copier_v0.modules.signal.ingest.event.Mt5SessionDisconnectedEvent;
import com.zyc.copier_v0.modules.signal.ingest.event.Mt5SignalAcceptedEvent;
import com.zyc.copier_v0.modules.signal.ingest.service.Mt5SessionRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AccountMonitorService {

    private final Mt5SignalRecordRepository signalRecordRepository;
    private final Mt5AccountRuntimeStateRepository runtimeStateRepository;
    private final Mt5AccountRepository mt5AccountRepository;
    private final CopyRelationRepository copyRelationRepository;
    private final FollowerDispatchOutboxRepository followerDispatchOutboxRepository;
    private final Mt5SessionRegistry mt5SessionRegistry;
    private final ObjectMapper objectMapper;
    private final MonitorProperties monitorProperties;

    public AccountMonitorService(
            Mt5SignalRecordRepository signalRecordRepository,
            Mt5AccountRuntimeStateRepository runtimeStateRepository,
            Mt5AccountRepository mt5AccountRepository,
            CopyRelationRepository copyRelationRepository,
            FollowerDispatchOutboxRepository followerDispatchOutboxRepository,
            Mt5SessionRegistry mt5SessionRegistry,
            ObjectMapper objectMapper,
            MonitorProperties monitorProperties
    ) {
        this.signalRecordRepository = signalRecordRepository;
        this.runtimeStateRepository = runtimeStateRepository;
        this.mt5AccountRepository = mt5AccountRepository;
        this.copyRelationRepository = copyRelationRepository;
        this.followerDispatchOutboxRepository = followerDispatchOutboxRepository;
        this.mt5SessionRegistry = mt5SessionRegistry;
        this.objectMapper = objectMapper;
        this.monitorProperties = monitorProperties;
    }

    @EventListener
    @Transactional
    public void onSignalAccepted(Mt5SignalAcceptedEvent event) {
        NormalizedMt5Signal signal = event.getSignal();
        Long accountId = resolveAccountId(signal.getServer(), signal.getLogin());

        Mt5SignalRecordEntity record = new Mt5SignalRecordEntity();
        record.setEventId(signal.getEventId());
        record.setSignalType(signal.getType().name());
        record.setSessionId(signal.getSessionId());
        record.setTraceId(signal.getTraceId());
        record.setAccountId(accountId);
        record.setLogin(signal.getLogin());
        record.setServer(signal.getServer());
        record.setAccountKey(signal.getMasterAccountKey());
        record.setSourceTimestamp(signal.getSourceTimestamp());
        record.setReceivedAt(signal.getReceivedAt());
        record.setPayloadJson(writePayload(signal));
        signalRecordRepository.save(record);

        if (signal.getLogin() == null || !StringUtils.hasText(signal.getServer())) {
            return;
        }

        Mt5AccountRuntimeStateEntity state = runtimeStateRepository.findByServerAndLogin(signal.getServer(), signal.getLogin())
                .orElseGet(Mt5AccountRuntimeStateEntity::new);
        state.setAccountId(accountId);
        state.setLogin(signal.getLogin());
        state.setServer(signal.getServer());
        state.setAccountKey(signal.getMasterAccountKey());
        state.setLastSessionId(signal.getSessionId());
        state.setConnectionStatus(Mt5ConnectionStatus.CONNECTED);
        state.setLastSignalAt(signal.getReceivedAt());
        state.setLastSignalType(signal.getType().name());
        state.setLastEventId(signal.getEventId());
        state.setBalance(readDecimal(signal.getPayload(), "account_balance", "balance"));
        state.setEquity(readDecimal(signal.getPayload(), "account_equity", "equity"));
        if ("HELLO".equals(signal.getType().name())) {
            state.setLastHelloAt(signal.getReceivedAt());
        }
        if ("HEARTBEAT".equals(signal.getType().name())) {
            state.setLastHeartbeatAt(signal.getReceivedAt());
        }
        runtimeStateRepository.save(state);
    }

    @EventListener
    @Transactional
    public void onSessionDisconnected(Mt5SessionDisconnectedEvent event) {
        Mt5SessionContext sessionContext = event.getSessionContext();
        if (sessionContext.getLogin() == null || !StringUtils.hasText(sessionContext.getServer())) {
            return;
        }

        runtimeStateRepository.findByServerAndLogin(sessionContext.getServer(), sessionContext.getLogin())
                .ifPresent(state -> {
                    state.setConnectionStatus(Mt5ConnectionStatus.DISCONNECTED);
                    state.setLastSessionId(sessionContext.getSessionId());
                    state.setLastSignalAt(event.getDisconnectedAt());
                    runtimeStateRepository.save(state);
                });
    }

    @Transactional(readOnly = true)
    public List<Mt5RuntimeStateResponse> listRuntimeStates() {
        return runtimeStateRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toRuntimeStateResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Mt5AccountMonitorOverviewResponse> listAccountOverviews(Long userId, Mt5AccountRole accountRole) {
        List<Mt5AccountEntity> accounts = mt5AccountRepository.findAllByOrderByIdAsc().stream()
                .filter(account -> userId == null || userId.equals(account.getUserId()))
                .filter(account -> accountRole == null || accountRole == account.getAccountRole())
                .toList();
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Mt5AccountRuntimeStateEntity> runtimeByAccountKey = runtimeStateRepository.findAll().stream()
                .collect(Collectors.toMap(
                        entity -> entity.getServer() + ":" + entity.getLogin(),
                        Function.identity(),
                        (left, right) -> left.getUpdatedAt().isAfter(right.getUpdatedAt()) ? left : right
                ));

        List<CopyRelationEntity> activeRelations = copyRelationRepository.findAllByStatusIn(EnumSet.of(CopyRelationStatus.ACTIVE));
        Map<Long, Long> followerCountByMaster = activeRelations.stream()
                .collect(Collectors.groupingBy(
                        relation -> relation.getMasterAccount().getId(),
                        Collectors.counting()
                ));
        Map<Long, Long> masterCountByFollower = activeRelations.stream()
                .collect(Collectors.groupingBy(
                        relation -> relation.getFollowerAccount().getId(),
                        Collectors.counting()
                ));

        List<FollowerDispatchOutboxEntity> dispatches = followerDispatchOutboxRepository.findAll();
        Map<Long, Long> pendingDispatchCount = dispatches.stream()
                .filter(dispatch -> dispatch.getStatus() == FollowerDispatchStatus.PENDING)
                .collect(Collectors.groupingBy(FollowerDispatchOutboxEntity::getFollowerAccountId, Collectors.counting()));
        Map<Long, Long> failedDispatchCount = dispatches.stream()
                .filter(dispatch -> dispatch.getStatus() == FollowerDispatchStatus.FAILED)
                .collect(Collectors.groupingBy(FollowerDispatchOutboxEntity::getFollowerAccountId, Collectors.counting()));

        return accounts.stream()
                .map(account -> toOverviewResponse(
                        account,
                        runtimeByAccountKey.get(account.getServerName() + ":" + account.getMt5Login()),
                        followerCountByMaster.getOrDefault(account.getId(), 0L),
                        masterCountByFollower.getOrDefault(account.getId(), 0L),
                        pendingDispatchCount.getOrDefault(account.getId(), 0L),
                        failedDispatchCount.getOrDefault(account.getId(), 0L)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Mt5WsSessionResponse> listWsSessions(Long userId, Mt5AccountRole accountRole) {
        Map<String, Mt5AccountEntity> accountByAccountKey = mt5AccountRepository.findAllByOrderByIdAsc().stream()
                .collect(Collectors.toMap(
                        account -> account.getServerName() + ":" + account.getMt5Login(),
                        Function.identity(),
                        (left, right) -> left
                ));
        List<Mt5AccountRuntimeStateEntity> runtimeStates = runtimeStateRepository.findAllByOrderByUpdatedAtDesc();
        Map<String, Mt5AccountRuntimeStateEntity> runtimeBySessionId = runtimeStates.stream()
                .filter(entity -> StringUtils.hasText(entity.getLastSessionId()))
                .collect(Collectors.toMap(
                        Mt5AccountRuntimeStateEntity::getLastSessionId,
                        Function.identity(),
                        (left, right) -> left
                ));
        Map<String, Mt5AccountRuntimeStateEntity> runtimeByAccountKey = runtimeStates.stream()
                .filter(entity -> StringUtils.hasText(entity.getAccountKey()))
                .collect(Collectors.toMap(
                        Mt5AccountRuntimeStateEntity::getAccountKey,
                        Function.identity(),
                        (left, right) -> left
                ));

        return mt5SessionRegistry.listAll().stream()
                .map(sessionContext -> toWsSessionResponse(
                        sessionContext,
                        accountByAccountKey,
                        runtimeBySessionId,
                        runtimeByAccountKey
                ))
                .filter(response -> includeWsSession(response, userId, accountRole, accountByAccountKey))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Mt5SignalRecordResponse> listSignalsByAccountId(Long accountId) {
        return signalRecordRepository.findTop50ByAccountIdOrderByReceivedAtDesc(accountId).stream()
                .map(this::toSignalRecordResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Mt5SignalRecordResponse> listSignalsByAccountKey(String accountKey) {
        if (!StringUtils.hasText(accountKey)) {
            return Collections.emptyList();
        }
        return signalRecordRepository.findTop50ByAccountKeyOrderByReceivedAtDesc(accountKey).stream()
                .map(this::toSignalRecordResponse)
                .toList();
    }

    private Long resolveAccountId(String server, Long login) {
        if (!StringUtils.hasText(server) || login == null) {
            return null;
        }
        Optional<Mt5AccountEntity> account = mt5AccountRepository.findByServerNameAndMt5Login(server, login);
        return account.map(Mt5AccountEntity::getId).orElse(null);
    }

    private String writePayload(NormalizedMt5Signal signal) {
        try {
            return objectMapper.writeValueAsString(signal.getPayload());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize signal payload", ex);
        }
    }

    private BigDecimal readDecimal(com.fasterxml.jackson.databind.JsonNode payload, String... fields) {
        for (String field : fields) {
            if (payload.hasNonNull(field)) {
                return payload.path(field).decimalValue();
            }
        }
        return null;
    }

    private Mt5RuntimeStateResponse toRuntimeStateResponse(Mt5AccountRuntimeStateEntity entity) {
        Mt5RuntimeStateResponse response = new Mt5RuntimeStateResponse();
        response.setId(entity.getId());
        response.setAccountId(entity.getAccountId());
        response.setLogin(entity.getLogin());
        response.setServer(entity.getServer());
        response.setAccountKey(entity.getAccountKey());
        response.setLastSessionId(entity.getLastSessionId());
        response.setConnectionStatus(resolveEffectiveStatus(entity));
        response.setLastHelloAt(entity.getLastHelloAt());
        response.setLastHeartbeatAt(entity.getLastHeartbeatAt());
        response.setLastSignalAt(entity.getLastSignalAt());
        response.setLastSignalType(entity.getLastSignalType());
        response.setLastEventId(entity.getLastEventId());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    private Mt5AccountMonitorOverviewResponse toOverviewResponse(
            Mt5AccountEntity account,
            Mt5AccountRuntimeStateEntity runtimeState,
            Long activeFollowerCount,
            Long activeMasterCount,
            Long pendingDispatchCount,
            Long failedDispatchCount
    ) {
        Mt5AccountMonitorOverviewResponse response = new Mt5AccountMonitorOverviewResponse();
        response.setAccountId(account.getId());
        response.setUserId(account.getUserId());
        response.setBrokerName(account.getBrokerName());
        response.setServerName(account.getServerName());
        response.setMt5Login(account.getMt5Login());
        response.setAccountKey(account.getServerName() + ":" + account.getMt5Login());
        response.setAccountRole(account.getAccountRole());
        response.setAccountStatus(account.getStatus());
        response.setActiveFollowerCount(activeFollowerCount);
        response.setActiveMasterCount(activeMasterCount);
        response.setPendingDispatchCount(pendingDispatchCount);
        response.setFailedDispatchCount(failedDispatchCount);
        if (runtimeState == null) {
            response.setConnectionStatus(Mt5ConnectionStatus.UNKNOWN);
            return response;
        }

        response.setConnectionStatus(resolveEffectiveStatus(runtimeState));
        response.setLastHelloAt(runtimeState.getLastHelloAt());
        response.setLastHeartbeatAt(runtimeState.getLastHeartbeatAt());
        response.setLastSignalAt(runtimeState.getLastSignalAt());
        response.setLastSignalType(runtimeState.getLastSignalType());
        response.setLastEventId(runtimeState.getLastEventId());
        response.setUpdatedAt(runtimeState.getUpdatedAt());
        return response;
    }

    private Mt5SignalRecordResponse toSignalRecordResponse(Mt5SignalRecordEntity entity) {
        Mt5SignalRecordResponse response = new Mt5SignalRecordResponse();
        response.setId(entity.getId());
        response.setEventId(entity.getEventId());
        response.setSignalType(entity.getSignalType());
        response.setAccountId(entity.getAccountId());
        response.setLogin(entity.getLogin());
        response.setServer(entity.getServer());
        response.setAccountKey(entity.getAccountKey());
        response.setSourceTimestamp(entity.getSourceTimestamp());
        response.setReceivedAt(entity.getReceivedAt());
        response.setPayloadJson(entity.getPayloadJson());
        return response;
    }

    private Mt5WsSessionResponse toWsSessionResponse(
            Mt5SessionContext sessionContext,
            Map<String, Mt5AccountEntity> accountByAccountKey,
            Map<String, Mt5AccountRuntimeStateEntity> runtimeBySessionId,
            Map<String, Mt5AccountRuntimeStateEntity> runtimeByAccountKey
    ) {
        String accountKey = sessionContext.masterAccountKey();
        Mt5AccountEntity account = accountKey == null ? null : accountByAccountKey.get(accountKey);
        Mt5AccountRuntimeStateEntity runtimeState = runtimeBySessionId.get(sessionContext.getSessionId());
        if (runtimeState == null && accountKey != null) {
            runtimeState = runtimeByAccountKey.get(accountKey);
        }

        Mt5WsSessionResponse response = new Mt5WsSessionResponse();
        response.setSessionId(sessionContext.getSessionId());
        response.setTraceId(sessionContext.getTraceId());
        response.setConnectedAt(sessionContext.getConnectedAt());
        response.setAccountId(runtimeState != null && runtimeState.getAccountId() != null
                ? runtimeState.getAccountId()
                : account != null ? account.getId() : null);
        response.setLogin(sessionContext.getLogin());
        response.setServer(sessionContext.getServer());
        response.setAccountKey(accountKey);
        response.setConnectionStatus(resolveSessionStatus(sessionContext, runtimeState));
        if (runtimeState != null) {
            response.setLastHelloAt(runtimeState.getLastHelloAt());
            response.setLastHeartbeatAt(runtimeState.getLastHeartbeatAt());
            response.setLastSignalAt(runtimeState.getLastSignalAt());
            response.setLastSignalType(runtimeState.getLastSignalType());
            response.setLastEventId(runtimeState.getLastEventId());
        }
        return response;
    }

    private boolean includeWsSession(
            Mt5WsSessionResponse response,
            Long userId,
            Mt5AccountRole accountRole,
            Map<String, Mt5AccountEntity> accountByAccountKey
    ) {
        if (userId == null && accountRole == null) {
            return true;
        }
        if (!StringUtils.hasText(response.getAccountKey())) {
            return false;
        }
        Mt5AccountEntity account = accountByAccountKey.get(response.getAccountKey());
        if (account == null) {
            return false;
        }
        if (userId != null && !userId.equals(account.getUserId())) {
            return false;
        }
        return accountRole == null || accountRole == account.getAccountRole();
    }

    private Mt5ConnectionStatus resolveSessionStatus(
            Mt5SessionContext sessionContext,
            Mt5AccountRuntimeStateEntity runtimeState
    ) {
        if (runtimeState != null) {
            return resolveEffectiveStatus(runtimeState);
        }
        if (sessionContext.getLogin() != null && StringUtils.hasText(sessionContext.getServer())) {
            return Mt5ConnectionStatus.CONNECTED;
        }
        return Mt5ConnectionStatus.UNKNOWN;
    }

    private Mt5ConnectionStatus resolveEffectiveStatus(Mt5AccountRuntimeStateEntity entity) {
        if (entity == null) {
            return Mt5ConnectionStatus.UNKNOWN;
        }
        if (entity.getConnectionStatus() == Mt5ConnectionStatus.DISCONNECTED) {
            return Mt5ConnectionStatus.DISCONNECTED;
        }

        Instant lastActivityAt = entity.getLastHeartbeatAt() != null
                ? entity.getLastHeartbeatAt()
                : entity.getLastSignalAt();
        if (lastActivityAt == null) {
            return entity.getConnectionStatus();
        }
        if (Instant.now().isAfter(lastActivityAt.plus(monitorProperties.getHeartbeatStaleAfter()))) {
            return Mt5ConnectionStatus.STALE;
        }
        return entity.getConnectionStatus();
    }
}
