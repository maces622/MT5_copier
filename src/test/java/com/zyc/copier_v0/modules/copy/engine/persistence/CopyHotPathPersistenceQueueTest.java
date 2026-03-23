package com.zyc.copier_v0.modules.copy.engine.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zyc.copier_v0.modules.copy.engine.config.CopyHotPathBackend;
import com.zyc.copier_v0.modules.copy.engine.config.CopyHotPathProperties;
import com.zyc.copier_v0.modules.copy.engine.entity.ExecutionCommandEntity;
import com.zyc.copier_v0.modules.monitor.entity.Mt5SignalRecordEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class CopyHotPathPersistenceQueueTest {

    @Mock
    private CopyHotPathKeyResolver keyResolver;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private CopyHotPathPersistenceService persistenceService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private CopyHotPathProperties properties;
    private CopyHotPathPersistenceQueue queue;

    @BeforeEach
    void setUp() {
        properties = new CopyHotPathProperties();
        properties.setBackend(CopyHotPathBackend.REDIS_QUEUE);
        lenient().when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        lenient().when(keyResolver.persistenceQueueKey()).thenReturn("copy:hot:persistence:queue");
        queue = new CopyHotPathPersistenceQueue(
                properties,
                keyResolver,
                stringRedisTemplate,
                objectMapper,
                persistenceService
        );
    }

    @Test
    void shouldPushPersistenceEnvelopeIntoRedisQueue() throws Exception {
        ExecutionCommandEntity command = new ExecutionCommandEntity();
        command.setId(101L);
        command.setMasterEventId("51631-DEAL-1");
        command.setFollowerAccountId(2L);
        when(listOperations.rightPush(eq("copy:hot:persistence:queue"), anyString())).thenReturn(1L);

        queue.enqueueExecutionCommand(command);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(listOperations).rightPush(eq("copy:hot:persistence:queue"), captor.capture());
        verify(persistenceService, never()).process(anyString());

        CopyHotPathPersistenceEnvelope envelope = objectMapper.readValue(
                captor.getValue(),
                CopyHotPathPersistenceEnvelope.class
        );
        assertThat(envelope.getType()).isEqualTo(CopyHotPathPersistenceMessageType.EXECUTION_COMMAND_UPSERT);
        ExecutionCommandEntity payload = objectMapper.readValue(envelope.getPayloadJson(), ExecutionCommandEntity.class);
        assertThat(payload.getId()).isEqualTo(101L);
        assertThat(payload.getMasterEventId()).isEqualTo("51631-DEAL-1");
    }

    @Test
    void shouldFallbackToInlinePersistenceWhenRedisEnqueueFails() throws Exception {
        Mt5SignalRecordEntity record = new Mt5SignalRecordEntity();
        record.setId(7L);
        record.setEventId("51631-DEAL-7");
        doThrow(new IllegalStateException("redis down"))
                .when(listOperations)
                .rightPush(eq("copy:hot:persistence:queue"), anyString());

        queue.enqueueSignalRecord(record);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(persistenceService).process(captor.capture());
        CopyHotPathPersistenceEnvelope envelope = objectMapper.readValue(
                captor.getValue(),
                CopyHotPathPersistenceEnvelope.class
        );
        assertThat(envelope.getType()).isEqualTo(CopyHotPathPersistenceMessageType.SIGNAL_RECORD_UPSERT);
        Mt5SignalRecordEntity payload = objectMapper.readValue(envelope.getPayloadJson(), Mt5SignalRecordEntity.class);
        assertThat(payload.getEventId()).isEqualTo("51631-DEAL-7");
    }

    @Test
    void shouldPersistInlineWhenDatabaseBackendIsConfigured() {
        properties.setBackend(CopyHotPathBackend.DATABASE);
        ExecutionCommandEntity command = new ExecutionCommandEntity();
        command.setId(102L);

        queue.enqueueExecutionCommand(command);

        verify(persistenceService).process(anyString());
        verify(listOperations, never()).rightPush(eq("copy:hot:persistence:queue"), anyString());
    }
}
