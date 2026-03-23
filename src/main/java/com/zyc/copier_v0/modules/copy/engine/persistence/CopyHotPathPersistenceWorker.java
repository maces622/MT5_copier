package com.zyc.copier_v0.modules.copy.engine.persistence;

import com.zyc.copier_v0.modules.copy.engine.config.CopyHotPathBackend;
import com.zyc.copier_v0.modules.copy.engine.config.CopyHotPathProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CopyHotPathPersistenceWorker {

    private static final Logger log = LoggerFactory.getLogger(CopyHotPathPersistenceWorker.class);

    private final CopyHotPathProperties properties;
    private final CopyHotPathKeyResolver keyResolver;
    private final StringRedisTemplate stringRedisTemplate;
    private final CopyHotPathPersistenceService persistenceService;

    public CopyHotPathPersistenceWorker(
            CopyHotPathProperties properties,
            CopyHotPathKeyResolver keyResolver,
            StringRedisTemplate stringRedisTemplate,
            CopyHotPathPersistenceService persistenceService
    ) {
        this.properties = properties;
        this.keyResolver = keyResolver;
        this.stringRedisTemplate = stringRedisTemplate;
        this.persistenceService = persistenceService;
    }

    @Scheduled(fixedDelayString = "${copier.copy-engine.hot-path.queue-worker-delay-ms:200}")
    public void drain() {
        if (properties.getBackend() != CopyHotPathBackend.REDIS_QUEUE) {
            return;
        }

        for (int i = 0; i < properties.getQueueWorkerBatchSize(); i++) {
            String raw = stringRedisTemplate.opsForList().leftPop(keyResolver.persistenceQueueKey());
            if (raw == null) {
                return;
            }
            try {
                persistenceService.process(raw);
            } catch (Exception ex) {
                log.error("Failed to process hot-path persistence message, moved to dead-letter queue", ex);
                stringRedisTemplate.opsForList().rightPush(keyResolver.persistenceDeadLetterKey(), raw);
            }
        }
    }
}
