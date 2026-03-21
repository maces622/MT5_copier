package com.zyc.copier_v0.modules.copy.followerexec.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.copy.engine.event.FollowerDispatchCreatedEvent;
import com.zyc.copier_v0.modules.copy.followerexec.config.FollowerDispatchRealtimeBackend;
import com.zyc.copier_v0.modules.copy.followerexec.config.FollowerDispatchRealtimeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class FollowerDispatchRealtimeCoordinatorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private FollowerExecWebSocketService followerExecWebSocketService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private FollowerDispatchRealtimeProperties properties;
    private FollowerDispatchRealtimeCoordinator coordinator;

    @BeforeEach
    void setUp() {
        properties = new FollowerDispatchRealtimeProperties();
        properties.setChannel("test:dispatch");
        properties.setNodeId("node-a");
        coordinator = new FollowerDispatchRealtimeCoordinator(
                objectMapper,
                stringRedisTemplate,
                properties,
                followerExecWebSocketService
        );
    }

    @Test
    void shouldPushLocallyWithoutPublishingWhenBackendIsLocal() {
        properties.setBackend(FollowerDispatchRealtimeBackend.LOCAL);

        coordinator.onFollowerDispatchCreated(new FollowerDispatchCreatedEvent(101L, 202L));

        verify(followerExecWebSocketService).tryPushPendingDispatch(202L, 101L);
        verify(stringRedisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void shouldPublishRedisMessageWhenBackendIsRedis() throws Exception {
        properties.setBackend(FollowerDispatchRealtimeBackend.REDIS);

        coordinator.onFollowerDispatchCreated(new FollowerDispatchCreatedEvent(101L, 202L));

        verify(followerExecWebSocketService).tryPushPendingDispatch(202L, 101L);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(stringRedisTemplate).convertAndSend(eq("test:dispatch"), payloadCaptor.capture());

        FollowerDispatchRealtimeMessage message = objectMapper.readValue(
                payloadCaptor.getValue(),
                FollowerDispatchRealtimeMessage.class
        );
        org.assertj.core.api.Assertions.assertThat(message.getDispatchId()).isEqualTo(101L);
        org.assertj.core.api.Assertions.assertThat(message.getFollowerAccountId()).isEqualTo(202L);
        org.assertj.core.api.Assertions.assertThat(message.getPublisherNodeId()).isEqualTo("node-a");
    }

    @Test
    void shouldKeepLocalPushWhenRedisPublishFails() {
        properties.setBackend(FollowerDispatchRealtimeBackend.REDIS);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(stringRedisTemplate)
                .convertAndSend(anyString(), anyString());

        coordinator.onFollowerDispatchCreated(new FollowerDispatchCreatedEvent(101L, 202L));

        verify(followerExecWebSocketService).tryPushPendingDispatch(202L, 101L);
        verify(stringRedisTemplate).convertAndSend(eq("test:dispatch"), anyString());
    }

    @Test
    void shouldIgnoreRedisMessagePublishedBySameNode() throws Exception {
        properties.setBackend(FollowerDispatchRealtimeBackend.REDIS);
        FollowerDispatchRealtimeMessage message = new FollowerDispatchRealtimeMessage();
        message.setDispatchId(101L);
        message.setFollowerAccountId(202L);
        message.setPublisherNodeId("node-a");

        coordinator.handlePublishedMessage(objectMapper.writeValueAsString(message));

        verifyNoInteractions(followerExecWebSocketService);
    }

    @Test
    void shouldPushDispatchWhenRedisMessageComesFromAnotherNode() throws Exception {
        properties.setBackend(FollowerDispatchRealtimeBackend.REDIS);
        FollowerDispatchRealtimeMessage message = new FollowerDispatchRealtimeMessage();
        message.setDispatchId(101L);
        message.setFollowerAccountId(202L);
        message.setPublisherNodeId("node-b");

        coordinator.handlePublishedMessage(objectMapper.writeValueAsString(message));

        verify(followerExecWebSocketService).tryPushPendingDispatch(202L, 101L);
    }
}
