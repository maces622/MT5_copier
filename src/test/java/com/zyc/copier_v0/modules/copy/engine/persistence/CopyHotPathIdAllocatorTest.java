package com.zyc.copier_v0.modules.copy.engine.persistence;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zyc.copier_v0.modules.copy.engine.config.CopyHotPathBackend;
import com.zyc.copier_v0.modules.copy.engine.config.CopyHotPathProperties;
import com.zyc.copier_v0.modules.copy.engine.repository.ExecutionCommandRepository;
import com.zyc.copier_v0.modules.copy.engine.repository.FollowerDispatchOutboxRepository;
import com.zyc.copier_v0.modules.monitor.repository.Mt5SignalRecordRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class CopyHotPathIdAllocatorTest {

    @Mock
    private CopyHotPathKeyResolver keyResolver;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ExecutionCommandRepository executionCommandRepository;

    @Mock
    private FollowerDispatchOutboxRepository followerDispatchOutboxRepository;

    @Mock
    private Mt5SignalRecordRepository mt5SignalRecordRepository;

    private CopyHotPathProperties properties;

    @BeforeEach
    void setUp() {
        properties = new CopyHotPathProperties();
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(keyResolver.commandSequenceKey()).thenReturn("copy:hot:seq:command");
        lenient().when(keyResolver.dispatchSequenceKey()).thenReturn("copy:hot:seq:dispatch");
        lenient().when(keyResolver.signalSequenceKey()).thenReturn("copy:hot:seq:signal");
    }

    @Test
    void runShouldWarmRedisSequencesFromDatabaseMaxIds() throws Exception {
        properties.setBackend(CopyHotPathBackend.REDIS_QUEUE);
        CopyHotPathIdAllocator allocator = new CopyHotPathIdAllocator(
                properties,
                keyResolver,
                stringRedisTemplate,
                executionCommandRepository,
                followerDispatchOutboxRepository,
                mt5SignalRecordRepository
        );

        when(executionCommandRepository.findMaxId()).thenReturn(Optional.of(120L));
        when(followerDispatchOutboxRepository.findMaxId()).thenReturn(Optional.of(45L));
        when(mt5SignalRecordRepository.findMaxId()).thenReturn(Optional.of(300L));
        when(valueOperations.get("copy:hot:seq:command")).thenReturn("10");
        when(valueOperations.get("copy:hot:seq:dispatch")).thenReturn(null);
        when(valueOperations.get("copy:hot:seq:signal")).thenReturn("300");

        allocator.run(new DefaultApplicationArguments(new String[0]));

        verify(valueOperations).set("copy:hot:seq:command", "120");
        verify(valueOperations).set("copy:hot:seq:dispatch", "45");
        verify(valueOperations, never()).set("copy:hot:seq:signal", "300");
    }
}
