package com.zyc.copier_v0.modules.signal.ingest.service;

import com.zyc.copier_v0.modules.signal.ingest.config.Mt5SignalDedupBackend;
import com.zyc.copier_v0.modules.signal.ingest.config.Mt5SignalIngestProperties;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class Mt5SignalDedupService {

    private static final Logger log = LoggerFactory.getLogger(Mt5SignalDedupService.class);

    private final Map<String, Instant> seenEventExpiry = new ConcurrentHashMap<>();
    private final Mt5SignalIngestProperties properties;
    private final StringRedisTemplate stringRedisTemplate;

    public Mt5SignalDedupService(Mt5SignalIngestProperties properties, StringRedisTemplate stringRedisTemplate) {
        this.properties = properties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean markIfNew(String eventId) {
        if (!StringUtils.hasText(eventId)) {
            return true;
        }

        if (properties.getDedupBackend() == Mt5SignalDedupBackend.REDIS) {
            Boolean redisMarked = markIfNewInRedis(eventId);
            if (redisMarked != null) {
                return redisMarked;
            }
        }

        return markIfNewInMemory(eventId);
    }

    private Boolean markIfNewInRedis(String eventId) {
        try {
            return stringRedisTemplate.opsForValue().setIfAbsent(
                    redisKey(eventId),
                    "1",
                    properties.getDedupTtl()
            );
        } catch (RuntimeException ex) {
            log.warn("Failed to deduplicate MT5 signal in redis, eventId={}, fallback=memory", eventId, ex);
            return null;
        }
    }

    private boolean markIfNewInMemory(String eventId) {
        Instant now = Instant.now();
        cleanupExpired(now);

        Instant expiry = now.plus(properties.getDedupTtl());
        Instant previous = seenEventExpiry.putIfAbsent(eventId, expiry);
        if (previous == null) {
            return true;
        }

        if (previous.isBefore(now)) {
            return seenEventExpiry.replace(eventId, previous, expiry);
        }

        return false;
    }

    private String redisKey(String eventId) {
        return properties.getDedupKeyPrefix() + ":" + eventId;
    }

    private void cleanupExpired(Instant now) {
        Iterator<Map.Entry<String, Instant>> iterator = seenEventExpiry.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Instant> entry = iterator.next();
            if (entry.getValue().isBefore(now)) {
                iterator.remove();
            }
        }
    }
}
