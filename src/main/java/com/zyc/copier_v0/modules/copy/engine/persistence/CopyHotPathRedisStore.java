package com.zyc.copier_v0.modules.copy.engine.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.copy.engine.config.CopyHotPathBackend;
import com.zyc.copier_v0.modules.copy.engine.config.CopyHotPathProperties;
import com.zyc.copier_v0.modules.copy.engine.domain.FollowerDispatchStatus;
import com.zyc.copier_v0.modules.copy.engine.entity.ExecutionCommandEntity;
import com.zyc.copier_v0.modules.copy.engine.entity.FollowerDispatchOutboxEntity;
import com.zyc.copier_v0.modules.copy.engine.repository.ExecutionCommandRepository;
import com.zyc.copier_v0.modules.copy.engine.repository.FollowerDispatchOutboxRepository;
import com.zyc.copier_v0.modules.monitor.entity.Mt5SignalRecordEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CopyHotPathRedisStore {

    private static final Logger log = LoggerFactory.getLogger(CopyHotPathRedisStore.class);

    private final CopyHotPathProperties properties;
    private final CopyHotPathKeyResolver keyResolver;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutionCommandRepository executionCommandRepository;
    private final FollowerDispatchOutboxRepository followerDispatchOutboxRepository;

    public CopyHotPathRedisStore(
            CopyHotPathProperties properties,
            CopyHotPathKeyResolver keyResolver,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            ExecutionCommandRepository executionCommandRepository,
            FollowerDispatchOutboxRepository followerDispatchOutboxRepository
    ) {
        this.properties = properties;
        this.keyResolver = keyResolver;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.executionCommandRepository = executionCommandRepository;
        this.followerDispatchOutboxRepository = followerDispatchOutboxRepository;
    }

    public boolean isRedisBackend() {
        return properties.getBackend() == CopyHotPathBackend.REDIS_QUEUE;
    }

    public Long nextCommandId() {
        return stringRedisTemplate.opsForValue().increment(keyResolver.commandSequenceKey());
    }

    public Long nextDispatchId() {
        return stringRedisTemplate.opsForValue().increment(keyResolver.dispatchSequenceKey());
    }

    public Long nextSignalRecordId() {
        return stringRedisTemplate.opsForValue().increment(keyResolver.signalSequenceKey());
    }

    public boolean reserveCommand(String masterEventId, Long followerAccountId, Long commandId) {
        if (!isRedisBackend()) {
            return false;
        }
        Boolean reserved = stringRedisTemplate.opsForValue().setIfAbsent(
                keyResolver.commandDedupKey(masterEventId, followerAccountId),
                String.valueOf(commandId)
        );
        return Boolean.TRUE.equals(reserved);
    }

    public void storeCommand(ExecutionCommandEntity command) {
        if (!isRedisBackend() || command == null || command.getId() == null) {
            return;
        }
        write(commandKey(command.getId()), command);
        addIndex(keyResolver.commandsByMasterEventKey(command.getMasterEventId()), command.getId());
        addIndex(keyResolver.commandsByFollowerKey(command.getFollowerAccountId()), command.getId());
        if (command.getMasterAccountId() != null) {
            addIndex(keyResolver.commandsByMasterAccountKey(command.getMasterAccountId()), command.getId());
            if (command.getMasterOrderId() != null) {
                addIndex(keyResolver.commandsByMasterOrderKey(command.getMasterAccountId(), command.getMasterOrderId()), command.getId());
            }
            if (command.getMasterPositionId() != null) {
                addIndex(keyResolver.commandsByMasterPositionKey(command.getMasterAccountId(), command.getMasterPositionId()), command.getId());
            }
        }
        stringRedisTemplate.opsForValue().set(
                keyResolver.commandDedupKey(command.getMasterEventId(), command.getFollowerAccountId()),
                String.valueOf(command.getId())
        );
    }

    public void storeDispatch(FollowerDispatchOutboxEntity dispatch) {
        if (!isRedisBackend() || dispatch == null || dispatch.getId() == null) {
            return;
        }
        Optional<FollowerDispatchOutboxEntity> previous = readDispatchFromCache(dispatch.getId());
        previous.ifPresent(this::removeDispatchStatusIndexes);
        write(dispatchKey(dispatch.getId()), dispatch);
        addIndex(keyResolver.dispatchByCommandKey(dispatch.getExecutionCommandId()), dispatch.getId());
        addIndex(keyResolver.dispatchesByFollowerKey(dispatch.getFollowerAccountId()), dispatch.getId());
        addIndex(keyResolver.dispatchesByMasterEventKey(dispatch.getMasterEventId()), dispatch.getId());
        addDispatchStatusIndexes(dispatch);
    }

    public Optional<FollowerDispatchOutboxEntity> findDispatchById(Long dispatchId) {
        if (!isRedisBackend() || dispatchId == null) {
            return Optional.empty();
        }
        Optional<FollowerDispatchOutboxEntity> cached = readDispatchFromCache(dispatchId);
        if (cached.isPresent()) {
            return cached;
        }
        return followerDispatchOutboxRepository.findById(dispatchId)
                .map(entity -> {
                    storeDispatch(entity);
                    return entity;
                });
    }

    public Optional<FollowerDispatchOutboxEntity> findDispatchByExecutionCommandId(Long executionCommandId) {
        if (!isRedisBackend() || executionCommandId == null) {
            return Optional.empty();
        }
        Optional<FollowerDispatchOutboxEntity> cached = loadDispatches(loadIds(keyResolver.dispatchByCommandKey(executionCommandId))).stream()
                .findFirst();
        if (cached.isPresent()) {
            return cached;
        }
        return followerDispatchOutboxRepository.findByExecutionCommandId(executionCommandId)
                .map(entity -> {
                    storeDispatch(entity);
                    return entity;
                });
    }

    public List<FollowerDispatchOutboxEntity> findPendingDispatchesByFollower(Long followerAccountId) {
        if (!isRedisBackend() || followerAccountId == null) {
            return Collections.emptyList();
        }
        List<FollowerDispatchOutboxEntity> cached = loadDispatches(loadIds(keyResolver.pendingDispatchesByFollowerKey(followerAccountId))).stream()
                .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
                .toList();
        if (!cached.isEmpty()) {
            return cached;
        }
        List<FollowerDispatchOutboxEntity> fromDatabase = followerDispatchOutboxRepository
                .findByFollowerAccountIdAndStatusOrderByIdAsc(followerAccountId, FollowerDispatchStatus.PENDING);
        fromDatabase.forEach(this::storeDispatch);
        return fromDatabase;
    }

    public List<FollowerDispatchOutboxEntity> findDispatchesByFollower(Long followerAccountId, FollowerDispatchStatus status) {
        if (!isRedisBackend() || followerAccountId == null) {
            return Collections.emptyList();
        }
        String indexKey = status == null
                ? keyResolver.dispatchesByFollowerKey(followerAccountId)
                : keyResolver.dispatchesByFollowerStatusKey(followerAccountId, status);
        List<FollowerDispatchOutboxEntity> cached = loadDispatches(loadIds(indexKey));
        if (!cached.isEmpty()) {
            return cached;
        }
        List<FollowerDispatchOutboxEntity> fromDatabase = status == null
                ? followerDispatchOutboxRepository.findByFollowerAccountIdOrderByIdDesc(followerAccountId)
                : followerDispatchOutboxRepository.findByFollowerAccountIdAndStatusOrderByIdAsc(followerAccountId, status);
        fromDatabase.forEach(this::storeDispatch);
        return fromDatabase;
    }

    public List<FollowerDispatchOutboxEntity> findDispatchesByMasterEventId(String masterEventId) {
        if (!isRedisBackend() || !StringUtils.hasText(masterEventId)) {
            return Collections.emptyList();
        }
        List<FollowerDispatchOutboxEntity> cached = loadDispatches(loadIds(keyResolver.dispatchesByMasterEventKey(masterEventId)));
        if (!cached.isEmpty()) {
            return cached;
        }
        List<FollowerDispatchOutboxEntity> fromDatabase = followerDispatchOutboxRepository.findByMasterEventIdOrderByIdAsc(masterEventId);
        fromDatabase.forEach(this::storeDispatch);
        return fromDatabase;
    }

    public List<FollowerDispatchOutboxEntity> findDispatchesByCommandIds(List<Long> commandIds) {
        if (!isRedisBackend() || commandIds == null || commandIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<FollowerDispatchOutboxEntity> dispatches = new ArrayList<>();
        for (Long commandId : commandIds) {
            dispatches.addAll(loadDispatches(loadIds(keyResolver.dispatchByCommandKey(commandId))));
        }
        if (!dispatches.isEmpty()) {
            Map<Long, FollowerDispatchOutboxEntity> byId = dispatches.stream()
                    .collect(Collectors.toMap(FollowerDispatchOutboxEntity::getId, value -> value, (left, right) -> left, LinkedHashMap::new));
            return new ArrayList<>(byId.values());
        }
        List<FollowerDispatchOutboxEntity> fromDatabase = followerDispatchOutboxRepository.findByExecutionCommandIdInOrderByIdAsc(commandIds);
        fromDatabase.forEach(this::storeDispatch);
        return fromDatabase;
    }

    public List<ExecutionCommandEntity> findCommandsByMasterEventId(String masterEventId) {
        if (!isRedisBackend() || !StringUtils.hasText(masterEventId)) {
            return Collections.emptyList();
        }
        List<ExecutionCommandEntity> cached = loadCommands(loadIds(keyResolver.commandsByMasterEventKey(masterEventId)));
        if (!cached.isEmpty()) {
            return cached;
        }
        List<ExecutionCommandEntity> fromDatabase = executionCommandRepository.findByMasterEventIdOrderByIdAsc(masterEventId);
        fromDatabase.forEach(this::storeCommand);
        return fromDatabase;
    }

    public List<ExecutionCommandEntity> findCommandsByFollower(Long followerAccountId) {
        if (!isRedisBackend() || followerAccountId == null) {
            return Collections.emptyList();
        }
        List<ExecutionCommandEntity> cached = loadCommands(loadIds(keyResolver.commandsByFollowerKey(followerAccountId)));
        if (!cached.isEmpty()) {
            return cached;
        }
        List<ExecutionCommandEntity> fromDatabase = executionCommandRepository.findByFollowerAccountIdOrderByIdDesc(followerAccountId);
        fromDatabase.forEach(this::storeCommand);
        return fromDatabase;
    }

    public List<ExecutionCommandEntity> findCommandsByMasterAccount(Long masterAccountId) {
        if (!isRedisBackend() || masterAccountId == null) {
            return Collections.emptyList();
        }
        List<ExecutionCommandEntity> cached = loadCommands(loadIds(keyResolver.commandsByMasterAccountKey(masterAccountId)));
        if (!cached.isEmpty()) {
            return cached;
        }
        List<ExecutionCommandEntity> fromDatabase = executionCommandRepository.findByMasterAccountIdOrderByIdDesc(masterAccountId);
        fromDatabase.forEach(this::storeCommand);
        return fromDatabase;
    }

    public List<ExecutionCommandEntity> findCommandsByMasterOrder(Long masterAccountId, Long masterOrderId) {
        if (!isRedisBackend() || masterAccountId == null || masterOrderId == null) {
            return Collections.emptyList();
        }
        List<ExecutionCommandEntity> cached = loadCommands(loadIds(keyResolver.commandsByMasterOrderKey(masterAccountId, masterOrderId)));
        if (!cached.isEmpty()) {
            return cached;
        }
        List<ExecutionCommandEntity> fromDatabase = executionCommandRepository
                .findByMasterAccountIdAndMasterOrderIdOrderByIdAsc(masterAccountId, masterOrderId);
        fromDatabase.forEach(this::storeCommand);
        return fromDatabase;
    }

    public List<ExecutionCommandEntity> findCommandsByMasterPosition(Long masterAccountId, Long masterPositionId) {
        if (!isRedisBackend() || masterAccountId == null || masterPositionId == null) {
            return Collections.emptyList();
        }
        List<ExecutionCommandEntity> cached = loadCommands(loadIds(keyResolver.commandsByMasterPositionKey(masterAccountId, masterPositionId)));
        if (!cached.isEmpty()) {
            return cached;
        }
        List<ExecutionCommandEntity> fromDatabase = executionCommandRepository
                .findByMasterAccountIdAndMasterPositionIdOrderByIdAsc(masterAccountId, masterPositionId);
        fromDatabase.forEach(this::storeCommand);
        return fromDatabase;
    }

    public void primeDispatch(FollowerDispatchOutboxEntity dispatch) {
        if (dispatch != null) {
            storeDispatch(dispatch);
        }
    }

    private List<ExecutionCommandEntity> loadCommands(List<Long> ids) {
        return ids.stream()
                .map(this::findCommandById)
                .flatMap(Optional::stream)
                .sorted((left, right) -> Long.compare(right.getId(), left.getId()))
                .toList();
    }

    private Optional<ExecutionCommandEntity> findCommandById(Long commandId) {
        String json = stringRedisTemplate.opsForValue().get(commandKey(commandId));
        if (StringUtils.hasText(json)) {
            return Optional.of(read(json, ExecutionCommandEntity.class));
        }
        return executionCommandRepository.findById(commandId)
                .map(entity -> {
                    storeCommand(entity);
                    return entity;
                });
    }

    private List<FollowerDispatchOutboxEntity> loadDispatches(List<Long> ids) {
        return ids.stream()
                .map(this::findDispatchById)
                .flatMap(Optional::stream)
                .sorted((left, right) -> Long.compare(right.getId(), left.getId()))
                .toList();
    }

    private Optional<FollowerDispatchOutboxEntity> readDispatchFromCache(Long dispatchId) {
        String json = stringRedisTemplate.opsForValue().get(dispatchKey(dispatchId));
        if (!StringUtils.hasText(json)) {
            return Optional.empty();
        }
        return Optional.of(read(json, FollowerDispatchOutboxEntity.class));
    }

    private List<Long> loadIds(String indexKey) {
        if (!isRedisBackend()) {
            return Collections.emptyList();
        }
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        List<String> values = Optional.ofNullable(zSetOperations.reverseRange(indexKey, 0, -1))
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
        return values.stream()
                .map(this::parseLong)
                .filter(value -> value != null && value > 0)
                .toList();
    }

    private void addDispatchStatusIndexes(FollowerDispatchOutboxEntity dispatch) {
        addIndex(keyResolver.dispatchesByFollowerStatusKey(dispatch.getFollowerAccountId(), dispatch.getStatus()), dispatch.getId());
        if (dispatch.getStatus() == FollowerDispatchStatus.PENDING) {
            addIndex(keyResolver.pendingDispatchesByFollowerKey(dispatch.getFollowerAccountId()), dispatch.getId());
        }
    }

    private void removeDispatchStatusIndexes(FollowerDispatchOutboxEntity dispatch) {
        removeIndex(keyResolver.dispatchesByFollowerStatusKey(dispatch.getFollowerAccountId(), dispatch.getStatus()), dispatch.getId());
        if (dispatch.getStatus() == FollowerDispatchStatus.PENDING) {
            removeIndex(keyResolver.pendingDispatchesByFollowerKey(dispatch.getFollowerAccountId()), dispatch.getId());
        }
    }

    private void addIndex(String key, Long id) {
        stringRedisTemplate.opsForZSet().add(key, String.valueOf(id), id.doubleValue());
    }

    private void removeIndex(String key, Long id) {
        stringRedisTemplate.opsForZSet().remove(key, String.valueOf(id));
    }

    private void write(String key, Object value) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize redis hot-path value", ex);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize redis hot-path value", ex);
        }
    }

    private String commandKey(Long id) {
        return keyResolver.commandKey(id);
    }

    private String dispatchKey(Long id) {
        return keyResolver.dispatchKey(id);
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Component
    static class PendingDispatchWarmupRunner implements ApplicationRunner {

        private final CopyHotPathProperties properties;
        private final FollowerDispatchOutboxRepository repository;
        private final CopyHotPathRedisStore store;

        PendingDispatchWarmupRunner(
                CopyHotPathProperties properties,
                FollowerDispatchOutboxRepository repository,
                CopyHotPathRedisStore store
        ) {
            this.properties = properties;
            this.repository = repository;
            this.store = store;
        }

        @Override
        public void run(ApplicationArguments args) {
            if (properties.getBackend() != CopyHotPathBackend.REDIS_QUEUE
                    || !properties.isWarmupPendingDispatchesOnStartup()) {
                return;
            }
            repository.findAll().stream()
                    .filter(dispatch -> dispatch.getStatus() == FollowerDispatchStatus.PENDING)
                    .forEach(store::primeDispatch);
        }
    }
}
