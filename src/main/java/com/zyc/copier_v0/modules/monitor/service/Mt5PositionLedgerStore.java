package com.zyc.copier_v0.modules.monitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.copy.engine.persistence.CopyHotPathPersistenceQueue;
import com.zyc.copier_v0.modules.monitor.config.Mt5RuntimeStateProperties;
import com.zyc.copier_v0.modules.monitor.entity.Mt5OpenPositionEntity;
import com.zyc.copier_v0.modules.monitor.repository.Mt5OpenPositionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class Mt5PositionLedgerStore {

    private static final Logger log = LoggerFactory.getLogger(Mt5PositionLedgerStore.class);
    private static final TypeReference<List<Mt5OpenPositionSnapshot>> SNAPSHOT_LIST = new TypeReference<>() {
    };

    private final Mt5RuntimeStateProperties properties;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Mt5OpenPositionRepository repository;
    private final CopyHotPathPersistenceQueue persistenceQueue;

    public Mt5PositionLedgerStore(
            Mt5RuntimeStateProperties properties,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            Mt5OpenPositionRepository repository,
            CopyHotPathPersistenceQueue persistenceQueue
    ) {
        this.properties = properties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.persistenceQueue = persistenceQueue;
    }

    public void reconcile(
            Long accountId,
            Long login,
            String server,
            String accountKey,
            List<Mt5OpenPositionSnapshot> positions,
            Instant observedAt
    ) {
        if (!StringUtils.hasText(accountKey)) {
            return;
        }

        List<Mt5OpenPositionSnapshot> normalized = normalize(positions, observedAt);
        List<Mt5OpenPositionSnapshot> current = findByAccountKey(accountKey);
        if (sameSnapshot(current, normalized)) {
            return;
        }

        writeSnapshot(accountKey, normalized);

        Mt5PositionLedgerReconcileMessage message = new Mt5PositionLedgerReconcileMessage();
        message.setAccountId(accountId);
        message.setLogin(login);
        message.setServer(server);
        message.setAccountKey(accountKey);
        message.setObservedAt(observedAt);
        message.setPositions(normalized);
        persistenceQueue.enqueuePositionLedger(message);
    }

    public List<Mt5OpenPositionSnapshot> findByAccountKey(String accountKey) {
        if (!StringUtils.hasText(accountKey)) {
            return Collections.emptyList();
        }

        try {
            String raw = stringRedisTemplate.opsForValue().get(positionKey(accountKey));
            if (StringUtils.hasText(raw)) {
                return normalize(objectMapper.readValue(raw, SNAPSHOT_LIST), Instant.now());
            }
        } catch (Exception ex) {
            log.warn("Failed to read position ledger from redis, accountKey={}", accountKey, ex);
        }

        List<Mt5OpenPositionSnapshot> fromDatabase = repository.findByAccountKeyOrderByPositionKeyAsc(accountKey).stream()
                .map(this::toSnapshot)
                .toList();
        if (!fromDatabase.isEmpty()) {
            writeSnapshot(accountKey, fromDatabase);
        }
        return fromDatabase;
    }

    public List<Mt5OpenPositionSnapshot> extractFromPayload(JsonNode payload, Instant observedAt) {
        if (payload == null || !payload.has("positions") || !payload.get("positions").isArray()) {
            return Collections.emptyList();
        }

        List<Mt5OpenPositionSnapshot> snapshots = new ArrayList<>();
        for (JsonNode item : payload.get("positions")) {
            Mt5OpenPositionSnapshot snapshot = new Mt5OpenPositionSnapshot();
            snapshot.setSourcePositionId(readLong(item, "position", "position_id", "ticket"));
            snapshot.setSourceOrderId(readLong(item, "order", "order_id"));
            snapshot.setSymbol(readText(item, "symbol"));
            snapshot.setVolume(readDecimal(item, "volume", "vol_cur"));
            snapshot.setPriceOpen(readDecimal(item, "price_open"));
            snapshot.setSl(readDecimal(item, "sl"));
            snapshot.setTp(readDecimal(item, "tp"));
            snapshot.setCommentText(readText(item, "comment"));
            snapshot.setObservedAt(observedAt);

            Map<String, Long> tracking = Mt5PositionCommentCodec.parseTrackingFields(snapshot.getCommentText());
            snapshot.setMasterPositionId(tracking.get("mp"));
            snapshot.setMasterOrderId(tracking.get("mo"));
            snapshot.setPositionKey(resolvePositionKey(snapshot));
            if (StringUtils.hasText(snapshot.getPositionKey())) {
                snapshots.add(snapshot);
            }
        }
        return normalize(snapshots, observedAt);
    }

    public boolean hasPositionsSnapshot(JsonNode payload) {
        return payload != null && payload.has("positions") && payload.get("positions").isArray();
    }

    private String resolvePositionKey(Mt5OpenPositionSnapshot snapshot) {
        if (snapshot.getMasterPositionId() != null && snapshot.getMasterPositionId() > 0) {
            return "mp:" + snapshot.getMasterPositionId();
        }
        if (snapshot.getMasterOrderId() != null && snapshot.getMasterOrderId() > 0) {
            return "mo:" + snapshot.getMasterOrderId();
        }
        if (snapshot.getSourcePositionId() != null && snapshot.getSourcePositionId() > 0) {
            return "sp:" + snapshot.getSourcePositionId();
        }
        if (snapshot.getSourceOrderId() != null && snapshot.getSourceOrderId() > 0) {
            return "so:" + snapshot.getSourceOrderId();
        }
        if (StringUtils.hasText(snapshot.getSymbol()) && StringUtils.hasText(snapshot.getCommentText())) {
            return "sc:" + snapshot.getSymbol() + ":" + snapshot.getCommentText();
        }
        return null;
    }

    private List<Mt5OpenPositionSnapshot> normalize(List<Mt5OpenPositionSnapshot> positions, Instant observedAt) {
        if (positions == null || positions.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Mt5OpenPositionSnapshot> byKey = new LinkedHashMap<>();
        for (Mt5OpenPositionSnapshot position : positions) {
            if (!StringUtils.hasText(position.getPositionKey())) {
                continue;
            }
            if (position.getObservedAt() == null) {
                position.setObservedAt(observedAt);
            }
            byKey.put(position.getPositionKey(), position);
        }
        return byKey.values().stream()
                .sorted(Comparator.comparing(Mt5OpenPositionSnapshot::getPositionKey))
                .toList();
    }

    private boolean sameSnapshot(List<Mt5OpenPositionSnapshot> left, List<Mt5OpenPositionSnapshot> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            Mt5OpenPositionSnapshot l = left.get(i);
            Mt5OpenPositionSnapshot r = right.get(i);
            if (!Objects.equals(l.getPositionKey(), r.getPositionKey())
                    || !Objects.equals(l.getSourcePositionId(), r.getSourcePositionId())
                    || !Objects.equals(l.getSourceOrderId(), r.getSourceOrderId())
                    || !Objects.equals(l.getMasterPositionId(), r.getMasterPositionId())
                    || !Objects.equals(l.getMasterOrderId(), r.getMasterOrderId())
                    || !Objects.equals(l.getSymbol(), r.getSymbol())
                    || compareDecimal(l.getVolume(), r.getVolume()) != 0
                    || compareDecimal(l.getPriceOpen(), r.getPriceOpen()) != 0
                    || compareDecimal(l.getSl(), r.getSl()) != 0
                    || compareDecimal(l.getTp(), r.getTp()) != 0
                    || !Objects.equals(trim(l.getCommentText()), trim(r.getCommentText()))) {
                return false;
            }
        }
        return true;
    }

    private int compareDecimal(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private void writeSnapshot(String accountKey, List<Mt5OpenPositionSnapshot> positions) {
        try {
            stringRedisTemplate.opsForValue().set(positionKey(accountKey), objectMapper.writeValueAsString(positions));
            stringRedisTemplate.opsForSet().add(indexKey(), accountKey);
        } catch (Exception ex) {
            log.warn("Failed to write position ledger to redis, accountKey={}", accountKey, ex);
        }
    }

    private String positionKey(String accountKey) {
        return properties.getKeyPrefix() + ":positions:" + accountKey;
    }

    private String indexKey() {
        return properties.getKeyPrefix() + ":positions:index";
    }

    private Mt5OpenPositionSnapshot toSnapshot(Mt5OpenPositionEntity entity) {
        Mt5OpenPositionSnapshot snapshot = new Mt5OpenPositionSnapshot();
        snapshot.setPositionKey(entity.getPositionKey());
        snapshot.setSourcePositionId(entity.getSourcePositionId());
        snapshot.setSourceOrderId(entity.getSourceOrderId());
        snapshot.setMasterPositionId(entity.getMasterPositionId());
        snapshot.setMasterOrderId(entity.getMasterOrderId());
        snapshot.setSymbol(entity.getSymbol());
        snapshot.setVolume(entity.getVolume());
        snapshot.setPriceOpen(entity.getPriceOpen());
        snapshot.setSl(entity.getSl());
        snapshot.setTp(entity.getTp());
        snapshot.setCommentText(entity.getCommentText());
        snapshot.setObservedAt(entity.getObservedAt());
        return snapshot;
    }

    private Long readLong(JsonNode payload, String... fields) {
        for (String field : fields) {
            JsonNode node = payload.get(field);
            if (node == null || node.isNull()) {
                continue;
            }
            if (node.isIntegralNumber()) {
                return node.longValue();
            }
            if (node.isTextual()) {
                try {
                    return Long.parseLong(node.textValue());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String readText(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        return node == null || node.isNull() ? null : trim(node.asText());
    }

    private BigDecimal readDecimal(JsonNode payload, String... fields) {
        for (String field : fields) {
            JsonNode node = payload.get(field);
            if (node == null || node.isNull()) {
                continue;
            }
            if (node.isNumber()) {
                return node.decimalValue();
            }
            if (node.isTextual() && StringUtils.hasText(node.textValue())) {
                try {
                    return new BigDecimal(node.textValue());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String trim(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Component
    static class WarmupRunner implements ApplicationRunner {

        private final Mt5RuntimeStateProperties properties;
        private final Mt5OpenPositionRepository repository;
        private final Mt5PositionLedgerStore store;

        WarmupRunner(
                Mt5RuntimeStateProperties properties,
                Mt5OpenPositionRepository repository,
                Mt5PositionLedgerStore store
        ) {
            this.properties = properties;
            this.repository = repository;
            this.store = store;
        }

        @Override
        public void run(ApplicationArguments args) {
            if (!properties.isWarmupOnStartup()) {
                return;
            }
            repository.findAllByOrderByAccountKeyAscPositionKeyAsc().stream()
                    .collect(Collectors.groupingBy(Mt5OpenPositionEntity::getAccountKey, LinkedHashMap::new, Collectors.toList()))
                    .forEach((accountKey, rows) -> store.writeSnapshot(
                            accountKey,
                            rows.stream().map(store::toSnapshot).toList()
                    ));
        }
    }
}
