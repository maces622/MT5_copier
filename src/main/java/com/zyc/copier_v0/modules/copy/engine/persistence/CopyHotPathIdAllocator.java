package com.zyc.copier_v0.modules.copy.engine.persistence;

import com.zyc.copier_v0.modules.copy.engine.config.CopyHotPathBackend;
import com.zyc.copier_v0.modules.copy.engine.config.CopyHotPathProperties;
import com.zyc.copier_v0.modules.copy.engine.repository.ExecutionCommandRepository;
import com.zyc.copier_v0.modules.copy.engine.repository.FollowerDispatchOutboxRepository;
import com.zyc.copier_v0.modules.monitor.repository.Mt5SignalRecordRepository;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CopyHotPathIdAllocator implements ApplicationRunner {

    private final CopyHotPathProperties properties;
    private final CopyHotPathKeyResolver keyResolver;
    private final StringRedisTemplate stringRedisTemplate;
    private final ExecutionCommandRepository executionCommandRepository;
    private final FollowerDispatchOutboxRepository followerDispatchOutboxRepository;
    private final Mt5SignalRecordRepository mt5SignalRecordRepository;

    private final AtomicLong commandSequence = new AtomicLong(0);
    private final AtomicLong dispatchSequence = new AtomicLong(0);
    private final AtomicLong signalSequence = new AtomicLong(0);

    public CopyHotPathIdAllocator(
            CopyHotPathProperties properties,
            CopyHotPathKeyResolver keyResolver,
            StringRedisTemplate stringRedisTemplate,
            ExecutionCommandRepository executionCommandRepository,
            FollowerDispatchOutboxRepository followerDispatchOutboxRepository,
            Mt5SignalRecordRepository mt5SignalRecordRepository
    ) {
        this.properties = properties;
        this.keyResolver = keyResolver;
        this.stringRedisTemplate = stringRedisTemplate;
        this.executionCommandRepository = executionCommandRepository;
        this.followerDispatchOutboxRepository = followerDispatchOutboxRepository;
        this.mt5SignalRecordRepository = mt5SignalRecordRepository;
    }

    public Long nextCommandId() {
        return properties.getBackend() == CopyHotPathBackend.REDIS_QUEUE
                ? stringRedisTemplate.opsForValue().increment(keyResolver.commandSequenceKey())
                : commandSequence.incrementAndGet();
    }

    public Long nextDispatchId() {
        return properties.getBackend() == CopyHotPathBackend.REDIS_QUEUE
                ? stringRedisTemplate.opsForValue().increment(keyResolver.dispatchSequenceKey())
                : dispatchSequence.incrementAndGet();
    }

    public Long nextSignalRecordId() {
        return properties.getBackend() == CopyHotPathBackend.REDIS_QUEUE
                ? stringRedisTemplate.opsForValue().increment(keyResolver.signalSequenceKey())
                : signalSequence.incrementAndGet();
    }

    @Override
    public void run(ApplicationArguments args) {
        long maxCommandId = executionCommandRepository.findMaxId().orElse(0L);
        long maxDispatchId = followerDispatchOutboxRepository.findMaxId().orElse(0L);
        long maxSignalId = mt5SignalRecordRepository.findMaxId().orElse(0L);
        if (properties.getBackend() == CopyHotPathBackend.REDIS_QUEUE) {
            ensureRedisSequenceAtLeast(keyResolver.commandSequenceKey(), maxCommandId);
            ensureRedisSequenceAtLeast(keyResolver.dispatchSequenceKey(), maxDispatchId);
            ensureRedisSequenceAtLeast(keyResolver.signalSequenceKey(), maxSignalId);
            return;
        }
        commandSequence.set(maxCommandId);
        dispatchSequence.set(maxDispatchId);
        signalSequence.set(maxSignalId);
    }

    private void ensureRedisSequenceAtLeast(String key, long requiredValue) {
        if (requiredValue <= 0) {
            return;
        }
        Long currentValue = parseLong(stringRedisTemplate.opsForValue().get(key));
        if (currentValue == null || currentValue < requiredValue) {
            stringRedisTemplate.opsForValue().set(key, String.valueOf(requiredValue));
        }
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
