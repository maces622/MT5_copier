package com.zyc.copier_v0.modules.copy.engine.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.copy.engine.config.CopyHotPathBackend;
import com.zyc.copier_v0.modules.copy.engine.config.CopyHotPathProperties;
import com.zyc.copier_v0.modules.copy.engine.entity.ExecutionCommandEntity;
import com.zyc.copier_v0.modules.copy.engine.entity.FollowerDispatchOutboxEntity;
import com.zyc.copier_v0.modules.monitor.entity.Mt5SignalRecordEntity;
import com.zyc.copier_v0.modules.monitor.service.Mt5PositionLedgerReconcileMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CopyHotPathPersistenceQueue {

    private static final Logger log = LoggerFactory.getLogger(CopyHotPathPersistenceQueue.class);

    private final CopyHotPathProperties properties;
    private final CopyHotPathKeyResolver keyResolver;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final CopyHotPathPersistenceService persistenceService;

    public CopyHotPathPersistenceQueue(
            CopyHotPathProperties properties,
            CopyHotPathKeyResolver keyResolver,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            CopyHotPathPersistenceService persistenceService
    ) {
        this.properties = properties;
        this.keyResolver = keyResolver;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.persistenceService = persistenceService;
    }

    public void enqueueSignalRecord(Mt5SignalRecordEntity record) {
        enqueue(CopyHotPathPersistenceMessageType.SIGNAL_RECORD_UPSERT, record);
    }

    public void enqueueExecutionCommand(ExecutionCommandEntity command) {
        enqueue(CopyHotPathPersistenceMessageType.EXECUTION_COMMAND_UPSERT, command);
    }

    public void enqueueFollowerDispatch(FollowerDispatchOutboxEntity dispatch) {
        enqueue(CopyHotPathPersistenceMessageType.FOLLOWER_DISPATCH_UPSERT, dispatch);
    }

    public void enqueuePositionLedger(Mt5PositionLedgerReconcileMessage message) {
        enqueue(CopyHotPathPersistenceMessageType.POSITION_LEDGER_RECONCILE, message);
    }

    private void enqueue(CopyHotPathPersistenceMessageType type, Object payload) {
        if (properties.getBackend() != CopyHotPathBackend.REDIS_QUEUE) {
            persistInline(type, payload);
            return;
        }

        try {
            CopyHotPathPersistenceEnvelope envelope = new CopyHotPathPersistenceEnvelope();
            envelope.setType(type);
            envelope.setPayloadJson(objectMapper.writeValueAsString(payload));
            stringRedisTemplate.opsForList().rightPush(
                    keyResolver.persistenceQueueKey(),
                    objectMapper.writeValueAsString(envelope)
            );
        } catch (Exception ex) {
            log.warn("Redis hot-path enqueue failed, fallback to inline persistence, type={}", type, ex);
            persistInline(type, payload);
        }
    }

    private void persistInline(CopyHotPathPersistenceMessageType type, Object payload) {
        try {
            CopyHotPathPersistenceEnvelope envelope = new CopyHotPathPersistenceEnvelope();
            envelope.setType(type);
            envelope.setPayloadJson(objectMapper.writeValueAsString(payload));
            persistenceService.process(objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize persistence payload", ex);
        }
    }
}
