package com.zyc.copier_v0.modules.monitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.monitor.config.Mt5RuntimeStateBackend;
import com.zyc.copier_v0.modules.monitor.config.Mt5RuntimeStateProperties;
import com.zyc.copier_v0.modules.monitor.entity.Mt5AccountRuntimeStateEntity;
import com.zyc.copier_v0.modules.monitor.repository.Mt5AccountRuntimeStateRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class Mt5AccountRuntimeStateStore {

    private final Mt5AccountRuntimeStateRepository repository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Mt5RuntimeStateProperties properties;

    public Mt5AccountRuntimeStateStore(
            Mt5AccountRuntimeStateRepository repository,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            Mt5RuntimeStateProperties properties
    ) {
        this.repository = repository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public Optional<Mt5AccountRuntimeStateSnapshot> findByAccountId(Long accountId) {
        if (accountId == null) {
            return Optional.empty();
        }
        if (!redisEnabled()) {
            return repository.findByAccountId(accountId).map(this::toSnapshot);
        }

        try {
            String accountKey = stringRedisTemplate.opsForValue().get(accountIdBindingKey(accountId));
            if (StringUtils.hasText(accountKey)) {
                Optional<Mt5AccountRuntimeStateSnapshot> cached = readFromRedis(accountKey);
                if (cached.isPresent()) {
                    return cached;
                }
                cleanupAccountIdBinding(accountId);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to read runtime-state account binding from redis, accountId={}, fallback=db", accountId, ex);
        }

        Optional<Mt5AccountRuntimeStateSnapshot> snapshot = repository.findByAccountId(accountId).map(this::toSnapshot);
        snapshot.ifPresent(this::cacheSnapshot);
        return snapshot;
    }

    public Optional<Mt5AccountRuntimeStateSnapshot> findFreshByAccountId(Long accountId) {
        return findByAccountId(accountId).filter(this::isFreshForFunds);
    }

    public Optional<Mt5AccountRuntimeStateSnapshot> findByServerAndLogin(String server, Long login) {
        if (!StringUtils.hasText(server) || login == null) {
            return Optional.empty();
        }
        if (!redisEnabled()) {
            return repository.findByServerAndLogin(server, login).map(this::toSnapshot);
        }

        Optional<Mt5AccountRuntimeStateSnapshot> cached = readFromRedis(accountKey(server, login));
        if (cached.isPresent()) {
            return cached;
        }

        Optional<Mt5AccountRuntimeStateSnapshot> snapshot = repository.findByServerAndLogin(server, login).map(this::toSnapshot);
        snapshot.ifPresent(this::cacheSnapshot);
        return snapshot;
    }

    public boolean isFreshForFunds(Mt5AccountRuntimeStateSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        if (!properties.isRequireFreshFundsForRatio()) {
            return true;
        }
        Instant freshnessReference = resolveFundsFreshnessReference(snapshot);
        if (freshnessReference == null) {
            return false;
        }
        Duration age = Duration.between(freshnessReference, Instant.now());
        return !age.isNegative() && age.compareTo(properties.getFundsStaleAfter()) <= 0;
    }

    public List<Mt5AccountRuntimeStateSnapshot> listAll() {
        if (!redisEnabled()) {
            return repository.findAllByOrderByUpdatedAtDesc().stream()
                    .map(this::toSnapshot)
                    .toList();
        }

        List<Mt5AccountRuntimeStateSnapshot> cached = readAllFromRedis();
        if (!cached.isEmpty()) {
            return cached;
        }

        List<Mt5AccountRuntimeStateSnapshot> snapshots = repository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toSnapshot)
                .toList();
        snapshots.forEach(this::cacheSnapshot);
        return snapshots;
    }

    public void upsert(Mt5AccountRuntimeStateSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        prepareSnapshot(snapshot);
        if (!redisEnabled()) {
            persistSnapshot(snapshot);
            return;
        }

        try {
            cacheSnapshot(snapshot);
        } catch (RuntimeException ex) {
            log.warn("Failed to cache runtime-state in redis, fallback=db, accountKey={}", snapshot.getAccountKey(), ex);
            persistSnapshot(snapshot);
        }
    }

    public void maybePersist(Mt5AccountRuntimeStateSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        prepareSnapshot(snapshot);
        if (!redisEnabled()) {
            persistSnapshot(snapshot);
            return;
        }

        try {
            Boolean allowed = stringRedisTemplate.opsForValue().setIfAbsent(
                    persistThrottleKey(snapshot.requireAccountKey()),
                    "1",
                    properties.getDatabaseSyncInterval()
            );
            if (Boolean.TRUE.equals(allowed)) {
                persistSnapshot(snapshot);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to throttle runtime-state db sync via redis, fallback=direct-db, accountKey={}",
                    snapshot.getAccountKey(), ex);
            persistSnapshot(snapshot);
        }
    }

    public void persist(Mt5AccountRuntimeStateSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        prepareSnapshot(snapshot);
        persistSnapshot(snapshot);
        touchPersistMarker(snapshot);
    }

    public int warmUpFromDatabase() {
        if (!redisEnabled() || !properties.isWarmupOnStartup()) {
            return 0;
        }
        List<Mt5AccountRuntimeStateSnapshot> snapshots = repository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toSnapshot)
                .toList();
        snapshots.forEach(this::cacheSnapshot);
        return snapshots.size();
    }

    private List<Mt5AccountRuntimeStateSnapshot> readAllFromRedis() {
        try {
            SetOperations<String, String> setOperations = stringRedisTemplate.opsForSet();
            Set<String> accountKeys = setOperations.members(indexKey());
            if (accountKeys == null || accountKeys.isEmpty()) {
                return List.of();
            }
            return accountKeys.stream()
                    .map(this::readFromRedis)
                    .flatMap(Optional::stream)
                    .sorted(Comparator.comparing(Mt5AccountRuntimeStateSnapshot::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                            .reversed())
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("Failed to load runtime-state index from redis, fallback=db", ex);
            return List.of();
        }
    }

    private Optional<Mt5AccountRuntimeStateSnapshot> readFromRedis(String accountKey) {
        if (!StringUtils.hasText(accountKey)) {
            return Optional.empty();
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(runtimeStateKey(accountKey));
            if (!StringUtils.hasText(json)) {
                cleanupIndexEntry(accountKey);
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, Mt5AccountRuntimeStateSnapshot.class));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize runtime-state from redis, accountKey={}", accountKey, ex);
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Failed to read runtime-state from redis, accountKey={}, fallback=db", accountKey, ex);
            return Optional.empty();
        }
    }

    private void cacheSnapshot(Mt5AccountRuntimeStateSnapshot snapshot) {
        if (!redisEnabled() || snapshot == null) {
            return;
        }
        String accountKey = snapshot.requireAccountKey();
        try {
            stringRedisTemplate.opsForValue().set(runtimeStateKey(accountKey), objectMapper.writeValueAsString(snapshot));
            stringRedisTemplate.opsForSet().add(indexKey(), accountKey);
            if (snapshot.getAccountId() != null) {
                stringRedisTemplate.opsForValue().set(accountIdBindingKey(snapshot.getAccountId()), accountKey);
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize runtime-state cache snapshot", ex);
        }
    }

    private void persistSnapshot(Mt5AccountRuntimeStateSnapshot snapshot) {
        Mt5AccountRuntimeStateEntity entity = repository.findByServerAndLogin(snapshot.getServer(), snapshot.getLogin())
                .orElseGet(Mt5AccountRuntimeStateEntity::new);
        entity.setAccountId(snapshot.getAccountId());
        entity.setLogin(snapshot.getLogin());
        entity.setServer(snapshot.getServer());
        entity.setAccountKey(snapshot.requireAccountKey());
        entity.setLastSessionId(snapshot.getLastSessionId());
        entity.setConnectionStatus(snapshot.getConnectionStatus());
        entity.setLastHelloAt(snapshot.getLastHelloAt());
        entity.setLastHeartbeatAt(snapshot.getLastHeartbeatAt());
        entity.setLastSignalAt(snapshot.getLastSignalAt());
        entity.setLastSignalType(snapshot.getLastSignalType());
        entity.setLastEventId(snapshot.getLastEventId());
        entity.setBalance(snapshot.getBalance());
        entity.setEquity(snapshot.getEquity());

        Mt5AccountRuntimeStateEntity saved = repository.save(entity);
        snapshot.setId(saved.getId());
        if (saved.getCreatedAt() != null) {
            snapshot.setCreatedAt(saved.getCreatedAt());
        }
        if (saved.getUpdatedAt() != null) {
            snapshot.setUpdatedAt(saved.getUpdatedAt());
        }
        if (redisEnabled()) {
            cacheSnapshot(snapshot);
        }
    }

    private Mt5AccountRuntimeStateSnapshot toSnapshot(Mt5AccountRuntimeStateEntity entity) {
        Mt5AccountRuntimeStateSnapshot snapshot = new Mt5AccountRuntimeStateSnapshot();
        snapshot.setId(entity.getId());
        snapshot.setAccountId(entity.getAccountId());
        snapshot.setLogin(entity.getLogin());
        snapshot.setServer(entity.getServer());
        snapshot.setAccountKey(entity.getAccountKey());
        snapshot.setLastSessionId(entity.getLastSessionId());
        snapshot.setConnectionStatus(entity.getConnectionStatus());
        snapshot.setLastHelloAt(entity.getLastHelloAt());
        snapshot.setLastHeartbeatAt(entity.getLastHeartbeatAt());
        snapshot.setLastSignalAt(entity.getLastSignalAt());
        snapshot.setLastSignalType(entity.getLastSignalType());
        snapshot.setLastEventId(entity.getLastEventId());
        snapshot.setBalance(entity.getBalance());
        snapshot.setEquity(entity.getEquity());
        snapshot.setCreatedAt(entity.getCreatedAt());
        snapshot.setUpdatedAt(entity.getUpdatedAt());
        return snapshot;
    }

    private void prepareSnapshot(Mt5AccountRuntimeStateSnapshot snapshot) {
        if (!StringUtils.hasText(snapshot.getAccountKey())
                && StringUtils.hasText(snapshot.getServer())
                && snapshot.getLogin() != null) {
            snapshot.setAccountKey(accountKey(snapshot.getServer(), snapshot.getLogin()));
        }
        Instant now = snapshot.getUpdatedAt() != null ? snapshot.getUpdatedAt() : Instant.now();
        if (snapshot.getCreatedAt() == null) {
            snapshot.setCreatedAt(now);
        }
        if (snapshot.getUpdatedAt() == null) {
            snapshot.setUpdatedAt(now);
        }
    }

    private void touchPersistMarker(Mt5AccountRuntimeStateSnapshot snapshot) {
        if (!redisEnabled() || snapshot == null || !StringUtils.hasText(snapshot.getAccountKey())) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    persistThrottleKey(snapshot.getAccountKey()),
                    "1",
                    properties.getDatabaseSyncInterval()
            );
        } catch (RuntimeException ex) {
            log.warn("Failed to update runtime-state db sync marker, accountKey={}", snapshot.getAccountKey(), ex);
        }
    }

    private void cleanupIndexEntry(String accountKey) {
        try {
            stringRedisTemplate.opsForSet().remove(indexKey(), accountKey);
        } catch (RuntimeException ex) {
            log.warn("Failed to cleanup runtime-state index entry, accountKey={}", accountKey, ex);
        }
    }

    private void cleanupAccountIdBinding(Long accountId) {
        try {
            stringRedisTemplate.delete(accountIdBindingKey(accountId));
        } catch (RuntimeException ex) {
            log.warn("Failed to cleanup runtime-state account binding, accountId={}", accountId, ex);
        }
    }

    private Instant resolveFundsFreshnessReference(Mt5AccountRuntimeStateSnapshot snapshot) {
        if (snapshot.getLastHeartbeatAt() != null) {
            return snapshot.getLastHeartbeatAt();
        }
        if (snapshot.getLastHelloAt() != null) {
            return snapshot.getLastHelloAt();
        }
        if (snapshot.getLastSignalAt() != null) {
            return snapshot.getLastSignalAt();
        }
        if (snapshot.getUpdatedAt() != null) {
            return snapshot.getUpdatedAt();
        }
        return snapshot.getCreatedAt();
    }

    private String accountKey(String server, Long login) {
        return server + ":" + login;
    }

    private String runtimeStateKey(String accountKey) {
        return properties.getKeyPrefix() + ":state:" + accountKey;
    }

    private String indexKey() {
        return properties.getKeyPrefix() + ":index";
    }

    private String accountIdBindingKey(Long accountId) {
        return properties.getKeyPrefix() + ":account:" + accountId;
    }

    private String persistThrottleKey(String accountKey) {
        return properties.getKeyPrefix() + ":db-sync:" + accountKey;
    }

    private boolean redisEnabled() {
        return properties.getBackend() == Mt5RuntimeStateBackend.REDIS;
    }
}
