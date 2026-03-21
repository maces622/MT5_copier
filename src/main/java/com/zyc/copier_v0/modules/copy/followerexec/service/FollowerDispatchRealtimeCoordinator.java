package com.zyc.copier_v0.modules.copy.followerexec.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.copy.engine.event.FollowerDispatchCreatedEvent;
import com.zyc.copier_v0.modules.copy.followerexec.config.FollowerDispatchRealtimeBackend;
import com.zyc.copier_v0.modules.copy.followerexec.config.FollowerDispatchRealtimeProperties;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class FollowerDispatchRealtimeCoordinator implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(FollowerDispatchRealtimeCoordinator.class);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final FollowerDispatchRealtimeProperties properties;
    private final FollowerExecWebSocketService followerExecWebSocketService;

    public FollowerDispatchRealtimeCoordinator(
            ObjectMapper objectMapper,
            StringRedisTemplate stringRedisTemplate,
            FollowerDispatchRealtimeProperties properties,
            FollowerExecWebSocketService followerExecWebSocketService
    ) {
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
        this.followerExecWebSocketService = followerExecWebSocketService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFollowerDispatchCreated(FollowerDispatchCreatedEvent event) {
        followerExecWebSocketService.tryPushPendingDispatch(event.getFollowerAccountId(), event.getDispatchId());
        publishToRedisIfEnabled(event);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (message == null || message.getBody() == null || message.getBody().length == 0) {
            return;
        }
        handlePublishedMessage(new String(message.getBody(), StandardCharsets.UTF_8));
    }

    void handlePublishedMessage(String payload) {
        if (properties.getBackend() != FollowerDispatchRealtimeBackend.REDIS) {
            return;
        }
        try {
            FollowerDispatchRealtimeMessage message = objectMapper.readValue(
                    payload,
                    FollowerDispatchRealtimeMessage.class
            );
            if (properties.getNodeId().equals(message.getPublisherNodeId())) {
                return;
            }
            followerExecWebSocketService.tryPushPendingDispatch(
                    message.getFollowerAccountId(),
                    message.getDispatchId()
            );
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize follower realtime dispatch message", ex);
        } catch (RuntimeException ex) {
            log.warn("Failed to handle follower realtime dispatch message", ex);
        }
    }

    private void publishToRedisIfEnabled(FollowerDispatchCreatedEvent event) {
        if (properties.getBackend() != FollowerDispatchRealtimeBackend.REDIS) {
            return;
        }
        try {
            FollowerDispatchRealtimeMessage message = new FollowerDispatchRealtimeMessage();
            message.setDispatchId(event.getDispatchId());
            message.setFollowerAccountId(event.getFollowerAccountId());
            message.setPublisherNodeId(properties.getNodeId());
            stringRedisTemplate.convertAndSend(properties.getChannel(), objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize follower realtime dispatch message", ex);
        } catch (RuntimeException ex) {
            log.warn("Failed to publish follower realtime dispatch message, fallback=local",
                    ex);
        }
    }
}
