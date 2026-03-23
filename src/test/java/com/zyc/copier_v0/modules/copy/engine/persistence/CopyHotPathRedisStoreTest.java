package com.zyc.copier_v0.modules.copy.engine.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zyc.copier_v0.modules.copy.engine.config.CopyHotPathBackend;
import com.zyc.copier_v0.modules.copy.engine.config.CopyHotPathProperties;
import com.zyc.copier_v0.modules.copy.engine.domain.FollowerDispatchStatus;
import com.zyc.copier_v0.modules.copy.engine.entity.FollowerDispatchOutboxEntity;
import com.zyc.copier_v0.modules.copy.engine.repository.ExecutionCommandRepository;
import com.zyc.copier_v0.modules.copy.engine.repository.FollowerDispatchOutboxRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class CopyHotPathRedisStoreTest {

    @Mock
    private CopyHotPathKeyResolver keyResolver;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private ExecutionCommandRepository executionCommandRepository;

    @Mock
    private FollowerDispatchOutboxRepository followerDispatchOutboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private CopyHotPathRedisStore store;

    @BeforeEach
    void setUp() {
        CopyHotPathProperties properties = new CopyHotPathProperties();
        properties.setBackend(CopyHotPathBackend.REDIS_QUEUE);

        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(keyResolver.dispatchKey(anyLong())).thenAnswer(invocation -> "copy:hot:dispatch:" + invocation.getArgument(0));
        lenient().when(keyResolver.dispatchByCommandKey(anyLong())).thenAnswer(invocation -> "copy:hot:dispatch:index:command:" + invocation.getArgument(0));
        lenient().when(keyResolver.dispatchesByFollowerKey(anyLong())).thenAnswer(invocation -> "copy:hot:dispatch:index:follower:" + invocation.getArgument(0));
        lenient().when(keyResolver.dispatchesByMasterEventKey(anyString())).thenAnswer(invocation -> "copy:hot:dispatch:index:event:" + invocation.getArgument(0));
        lenient().when(keyResolver.dispatchesByFollowerStatusKey(anyLong(), any(FollowerDispatchStatus.class)))
                .thenAnswer(invocation -> "copy:hot:dispatch:index:follower-status:" + invocation.getArgument(0) + ":" + invocation.getArgument(1));
        lenient().when(keyResolver.pendingDispatchesByFollowerKey(anyLong()))
                .thenAnswer(invocation -> "copy:hot:dispatch:index:follower-pending:" + invocation.getArgument(0));

        store = new CopyHotPathRedisStore(
                properties,
                keyResolver,
                stringRedisTemplate,
                objectMapper,
                executionCommandRepository,
                followerDispatchOutboxRepository
        );
    }

    @Test
    void storeDispatchShouldOnlyUseRedisSnapshotForPreviousState() throws Exception {
        FollowerDispatchOutboxEntity previous = dispatch(7L, 11L, 22L, "event-7", FollowerDispatchStatus.PENDING);
        FollowerDispatchOutboxEntity updated = dispatch(7L, 11L, 22L, "event-7", FollowerDispatchStatus.ACKED);
        when(valueOperations.get("copy:hot:dispatch:7")).thenReturn(objectMapper.writeValueAsString(previous));

        store.storeDispatch(updated);

        verify(followerDispatchOutboxRepository, never()).findById(anyLong());
        verify(zSetOperations).remove("copy:hot:dispatch:index:follower-status:22:PENDING", "7");
        verify(zSetOperations).remove("copy:hot:dispatch:index:follower-pending:22", "7");
        verify(zSetOperations).add("copy:hot:dispatch:index:follower-status:22:ACKED", "7", 7.0d);
        verify(valueOperations).set(eq("copy:hot:dispatch:7"), anyString());
    }

    @Test
    void findDispatchByIdShouldHydrateCacheWithoutRecursiveRepositoryLoop() {
        FollowerDispatchOutboxEntity dispatch = dispatch(9L, 21L, 33L, "event-9", FollowerDispatchStatus.PENDING);
        when(followerDispatchOutboxRepository.findById(9L)).thenReturn(Optional.of(dispatch));

        Optional<FollowerDispatchOutboxEntity> result = store.findDispatchById(9L);

        assertThat(result).contains(dispatch);
        verify(followerDispatchOutboxRepository, times(1)).findById(9L);
        verify(valueOperations).set(eq("copy:hot:dispatch:9"), anyString());
    }

    @Test
    void findDispatchByExecutionCommandIdShouldHydrateCacheFromRepository() {
        FollowerDispatchOutboxEntity dispatch = dispatch(12L, 34L, 56L, "event-12", FollowerDispatchStatus.PENDING);
        when(followerDispatchOutboxRepository.findByExecutionCommandId(34L)).thenReturn(Optional.of(dispatch));

        Optional<FollowerDispatchOutboxEntity> result = store.findDispatchByExecutionCommandId(34L);

        assertThat(result).contains(dispatch);
        verify(followerDispatchOutboxRepository).findByExecutionCommandId(34L);
        verify(valueOperations).set(eq("copy:hot:dispatch:12"), anyString());
    }

    private FollowerDispatchOutboxEntity dispatch(
            Long id,
            Long executionCommandId,
            Long followerAccountId,
            String masterEventId,
            FollowerDispatchStatus status
    ) {
        FollowerDispatchOutboxEntity dispatch = new FollowerDispatchOutboxEntity();
        dispatch.setId(id);
        dispatch.setExecutionCommandId(executionCommandId);
        dispatch.setFollowerAccountId(followerAccountId);
        dispatch.setMasterEventId(masterEventId);
        dispatch.setStatus(status);
        dispatch.setPayloadJson("{\"id\":" + id + "}");
        return dispatch;
    }
}
