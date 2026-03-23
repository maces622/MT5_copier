package com.zyc.copier_v0.modules.copy.engine.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.copy.engine.entity.ExecutionCommandEntity;
import com.zyc.copier_v0.modules.copy.engine.entity.FollowerDispatchOutboxEntity;
import com.zyc.copier_v0.modules.copy.engine.repository.ExecutionCommandRepository;
import com.zyc.copier_v0.modules.copy.engine.repository.FollowerDispatchOutboxRepository;
import com.zyc.copier_v0.modules.monitor.entity.Mt5SignalRecordEntity;
import com.zyc.copier_v0.modules.monitor.repository.Mt5SignalRecordRepository;
import com.zyc.copier_v0.modules.monitor.service.Mt5PositionLedgerReconcileMessage;
import com.zyc.copier_v0.modules.monitor.service.Mt5PositionLedgerPersistenceService;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CopyHotPathPersistenceService {

    private final ObjectMapper objectMapper;
    private final Mt5SignalRecordRepository signalRecordRepository;
    private final ExecutionCommandRepository executionCommandRepository;
    private final FollowerDispatchOutboxRepository followerDispatchOutboxRepository;
    private final Mt5PositionLedgerPersistenceService positionLedgerPersistenceService;

    public CopyHotPathPersistenceService(
            ObjectMapper objectMapper,
            Mt5SignalRecordRepository signalRecordRepository,
            ExecutionCommandRepository executionCommandRepository,
            FollowerDispatchOutboxRepository followerDispatchOutboxRepository,
            Mt5PositionLedgerPersistenceService positionLedgerPersistenceService
    ) {
        this.objectMapper = objectMapper;
        this.signalRecordRepository = signalRecordRepository;
        this.executionCommandRepository = executionCommandRepository;
        this.followerDispatchOutboxRepository = followerDispatchOutboxRepository;
        this.positionLedgerPersistenceService = positionLedgerPersistenceService;
    }

    @Transactional
    public void process(String rawEnvelope) {
        CopyHotPathPersistenceEnvelope envelope = read(rawEnvelope, CopyHotPathPersistenceEnvelope.class);
        switch (envelope.getType()) {
            case SIGNAL_RECORD_UPSERT -> upsertSignalRecord(read(envelope.getPayloadJson(), Mt5SignalRecordEntity.class));
            case EXECUTION_COMMAND_UPSERT -> upsertExecutionCommand(read(envelope.getPayloadJson(), ExecutionCommandEntity.class));
            case FOLLOWER_DISPATCH_UPSERT -> upsertFollowerDispatch(read(envelope.getPayloadJson(), FollowerDispatchOutboxEntity.class));
            case POSITION_LEDGER_RECONCILE -> positionLedgerPersistenceService.persistReconcileMessage(
                    read(envelope.getPayloadJson(), Mt5PositionLedgerReconcileMessage.class)
            );
            default -> throw new IllegalArgumentException("Unsupported persistence message type: " + envelope.getType());
        }
    }

    @Transactional
    public void upsertSignalRecord(Mt5SignalRecordEntity snapshot) {
        Optional<Mt5SignalRecordEntity> existing = signalRecordRepository.findById(snapshot.getId());
        Mt5SignalRecordEntity entity = existing.orElseGet(Mt5SignalRecordEntity::new);
        entity.setId(snapshot.getId());
        entity.setEventId(snapshot.getEventId());
        entity.setSignalType(snapshot.getSignalType());
        entity.setSessionId(snapshot.getSessionId());
        entity.setTraceId(snapshot.getTraceId());
        entity.setAccountId(snapshot.getAccountId());
        entity.setLogin(snapshot.getLogin());
        entity.setServer(snapshot.getServer());
        entity.setAccountKey(snapshot.getAccountKey());
        entity.setSourceTimestamp(snapshot.getSourceTimestamp());
        entity.setReceivedAt(snapshot.getReceivedAt());
        entity.setPayloadJson(snapshot.getPayloadJson());
        signalRecordRepository.save(entity);
    }

    @Transactional
    public void upsertExecutionCommand(ExecutionCommandEntity snapshot) {
        Optional<ExecutionCommandEntity> existing = executionCommandRepository.findById(snapshot.getId());
        ExecutionCommandEntity entity = existing.orElseGet(ExecutionCommandEntity::new);
        entity.setId(snapshot.getId());
        entity.setMasterEventId(snapshot.getMasterEventId());
        entity.setMasterAccountId(snapshot.getMasterAccountId());
        entity.setMasterAccountKey(snapshot.getMasterAccountKey());
        entity.setFollowerAccountId(snapshot.getFollowerAccountId());
        entity.setMasterSymbol(snapshot.getMasterSymbol());
        entity.setSignalType(snapshot.getSignalType());
        entity.setCommandType(snapshot.getCommandType());
        entity.setSymbol(snapshot.getSymbol());
        entity.setMasterAction(snapshot.getMasterAction());
        entity.setFollowerAction(snapshot.getFollowerAction());
        entity.setCopyMode(snapshot.getCopyMode());
        entity.setRequestedVolume(snapshot.getRequestedVolume());
        entity.setRequestedPrice(snapshot.getRequestedPrice());
        entity.setRequestedSl(snapshot.getRequestedSl());
        entity.setRequestedTp(snapshot.getRequestedTp());
        entity.setMasterDealId(snapshot.getMasterDealId());
        entity.setMasterOrderId(snapshot.getMasterOrderId());
        entity.setMasterPositionId(snapshot.getMasterPositionId());
        entity.setStatus(snapshot.getStatus());
        entity.setRejectReason(snapshot.getRejectReason());
        entity.setRejectMessage(snapshot.getRejectMessage());
        entity.setSignalTime(snapshot.getSignalTime());
        executionCommandRepository.save(entity);
    }

    @Transactional
    public void upsertFollowerDispatch(FollowerDispatchOutboxEntity snapshot) {
        Optional<FollowerDispatchOutboxEntity> existing = followerDispatchOutboxRepository.findById(snapshot.getId());
        if (!existing.isPresent() && snapshot.getExecutionCommandId() != null) {
            existing = followerDispatchOutboxRepository.findByExecutionCommandId(snapshot.getExecutionCommandId());
        }
        FollowerDispatchOutboxEntity entity = existing.orElseGet(FollowerDispatchOutboxEntity::new);
        entity.setId(existing.map(FollowerDispatchOutboxEntity::getId).orElse(snapshot.getId()));
        entity.setExecutionCommandId(snapshot.getExecutionCommandId());
        entity.setMasterEventId(snapshot.getMasterEventId());
        entity.setFollowerAccountId(snapshot.getFollowerAccountId());
        entity.setStatus(snapshot.getStatus());
        entity.setStatusMessage(snapshot.getStatusMessage());
        entity.setPayloadJson(snapshot.getPayloadJson());
        entity.setAckedAt(snapshot.getAckedAt());
        entity.setFailedAt(snapshot.getFailedAt());
        followerDispatchOutboxRepository.save(entity);
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize persistence payload", ex);
        }
    }
}
