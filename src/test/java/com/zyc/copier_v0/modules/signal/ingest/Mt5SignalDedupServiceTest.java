package com.zyc.copier_v0.modules.signal.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.zyc.copier_v0.modules.signal.ingest.config.Mt5SignalDedupBackend;
import com.zyc.copier_v0.modules.signal.ingest.config.Mt5SignalIngestProperties;
import com.zyc.copier_v0.modules.signal.ingest.service.Mt5SignalDedupService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class Mt5SignalDedupServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private Mt5SignalIngestProperties properties;

    @BeforeEach
    void setUp() {
        properties = new Mt5SignalIngestProperties();
        properties.setDedupTtl(Duration.ofSeconds(1));
        properties.setDedupKeyPrefix("copy:signal:dedup");
    }

    @Test
    void shouldUseRedisTtlWhenRedisBackendIsEnabled() {
        properties.setDedupBackend(Mt5SignalDedupBackend.REDIS);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("copy:signal:dedup:event-1"), eq("1"), eq(Duration.ofSeconds(1))))
                .thenReturn(true)
                .thenReturn(false);

        Mt5SignalDedupService service = new Mt5SignalDedupService(properties, stringRedisTemplate);

        assertThat(service.markIfNew("event-1")).isTrue();
        assertThat(service.markIfNew("event-1")).isFalse();
    }

    @Test
    void shouldFallbackToMemoryWhenRedisFails() {
        properties.setDedupBackend(Mt5SignalDedupBackend.REDIS);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("copy:signal:dedup:event-2"), eq("1"), eq(Duration.ofSeconds(1))))
                .thenThrow(new IllegalStateException("redis unavailable"));

        Mt5SignalDedupService service = new Mt5SignalDedupService(properties, stringRedisTemplate);

        assertThat(service.markIfNew("event-2")).isTrue();
        assertThat(service.markIfNew("event-2")).isFalse();
    }

    @Test
    void shouldExpireEntriesWhenUsingMemoryBackend() throws Exception {
        properties.setDedupBackend(Mt5SignalDedupBackend.MEMORY);
        properties.setDedupTtl(Duration.ofMillis(50));

        Mt5SignalDedupService service = new Mt5SignalDedupService(properties, stringRedisTemplate);

        assertThat(service.markIfNew("event-3")).isTrue();
        assertThat(service.markIfNew("event-3")).isFalse();
        Thread.sleep(80L);
        assertThat(service.markIfNew("event-3")).isTrue();
    }
}
