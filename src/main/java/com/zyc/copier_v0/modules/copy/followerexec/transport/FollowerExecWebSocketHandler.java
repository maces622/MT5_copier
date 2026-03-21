package com.zyc.copier_v0.modules.copy.followerexec.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zyc.copier_v0.modules.copy.followerexec.security.FollowerExecHandshakeInterceptor;
import com.zyc.copier_v0.modules.copy.followerexec.service.FollowerExecWebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class FollowerExecWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(FollowerExecWebSocketHandler.class);

    private final FollowerExecWebSocketService followerExecWebSocketService;

    public FollowerExecWebSocketHandler(FollowerExecWebSocketService followerExecWebSocketService) {
        this.followerExecWebSocketService = followerExecWebSocketService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        followerExecWebSocketService.registerConnection(session.getId(), traceId(session), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            followerExecWebSocketService.handleMessage(session.getId(), traceId(session), message.getPayload());
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            log.warn("Close invalid follower-exec websocket payload, sessionId={}, traceId={}, reason={}",
                    session.getId(), traceId(session), ex.getMessage());
            session.close(CloseStatus.BAD_DATA.withReason("invalid follower-exec payload"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("Follower-exec websocket transport error, sessionId={}, traceId={}, reason={}",
                session.getId(), traceId(session), exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        followerExecWebSocketService.unregisterConnection(session.getId());
    }

    private String traceId(WebSocketSession session) {
        Object traceId = session.getAttributes().get(FollowerExecHandshakeInterceptor.TRACE_ID_ATTR);
        return traceId == null ? session.getId() : traceId.toString();
    }
}
