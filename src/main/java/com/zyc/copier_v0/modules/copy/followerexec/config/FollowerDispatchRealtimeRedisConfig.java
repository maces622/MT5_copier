package com.zyc.copier_v0.modules.copy.followerexec.config;

import com.zyc.copier_v0.modules.copy.followerexec.service.FollowerDispatchRealtimeCoordinator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class FollowerDispatchRealtimeRedisConfig {

    @Bean
    @ConditionalOnProperty(
            prefix = "copier.mt5.follower-exec.realtime-dispatch",
            name = "backend",
            havingValue = "redis"
    )
    public RedisMessageListenerContainer followerDispatchRealtimeRedisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            FollowerDispatchRealtimeCoordinator coordinator,
            FollowerDispatchRealtimeProperties properties
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(coordinator, new ChannelTopic(properties.getChannel()));
        return container;
    }
}
