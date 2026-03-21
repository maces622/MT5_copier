package com.zyc.copier_v0.modules.signal.ingest.config;

import com.zyc.copier_v0.modules.signal.ingest.security.Mt5HandshakeInterceptor;
import com.zyc.copier_v0.modules.signal.ingest.transport.Mt5TradeWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class Mt5SignalWebSocketConfig implements WebSocketConfigurer {

    private final Mt5TradeWebSocketHandler tradeWebSocketHandler;
    private final Mt5HandshakeInterceptor handshakeInterceptor;
    private final Mt5SignalIngestProperties properties;

    public Mt5SignalWebSocketConfig(
            Mt5TradeWebSocketHandler tradeWebSocketHandler,
            Mt5HandshakeInterceptor handshakeInterceptor,
            Mt5SignalIngestProperties properties
    ) {
        this.tradeWebSocketHandler = tradeWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.properties = properties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(tradeWebSocketHandler, properties.getPath())
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
