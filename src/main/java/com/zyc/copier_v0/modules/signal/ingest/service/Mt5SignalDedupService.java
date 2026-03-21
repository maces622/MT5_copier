package com.zyc.copier_v0.modules.signal.ingest.service;

import com.zyc.copier_v0.modules.signal.ingest.config.Mt5SignalIngestProperties;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class Mt5SignalDedupService {

    private final Map<String, Instant> seenEventExpiry = new ConcurrentHashMap<>();
    private final Mt5SignalIngestProperties properties;

    public Mt5SignalDedupService(Mt5SignalIngestProperties properties) {
        this.properties = properties;
    }

    public boolean markIfNew(String eventId) {
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
