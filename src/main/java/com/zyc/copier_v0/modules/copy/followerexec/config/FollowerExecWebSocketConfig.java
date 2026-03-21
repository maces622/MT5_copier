package com.zyc.copier_v0.modules.copy.followerexec.config;

import com.zyc.copier_v0.modules.copy.followerexec.security.FollowerExecHandshakeInterceptor;
import com.zyc.copier_v0.modules.copy.followerexec.transport.FollowerExecWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class FollowerExecWebSocketConfig implements WebSocketConfigurer {

    private final FollowerExecWebSocketHandler followerExecWebSocketHandler;
    private final FollowerExecHandshakeInterceptor handshakeInterceptor;
    private final FollowerExecWebSocketProperties properties;

    public FollowerExecWebSocketConfig(
            FollowerExecWebSocketHandler followerExecWebSocketHandler,
            FollowerExecHandshakeInterceptor handshakeInterceptor,
            FollowerExecWebSocketProperties properties
    ) {
        this.followerExecWebSocketHandler = followerExecWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.properties = properties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(followerExecWebSocketHandler, properties.getPath())
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
