package com.zyc.copier_v0.modules.signal.ingest.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zyc.copier_v0.modules.signal.ingest.security.Mt5HandshakeInterceptor;
import com.zyc.copier_v0.modules.signal.ingest.service.Mt5SignalIngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class Mt5TradeWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(Mt5TradeWebSocketHandler.class);

    private final Mt5SignalIngestService signalIngestService;

    public Mt5TradeWebSocketHandler(Mt5SignalIngestService signalIngestService) {
        this.signalIngestService = signalIngestService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        signalIngestService.registerConnection(session.getId(), traceId(session));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            signalIngestService.ingest(session.getId(), traceId(session), message.getPayload());
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            log.warn("Close invalid MT5 websocket payload, sessionId={}, traceId={}, reason={}",
                    session.getId(), traceId(session), ex.getMessage());
            session.close(CloseStatus.BAD_DATA.withReason("invalid mt5 payload"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("MT5 websocket transport error, sessionId={}, traceId={}, reason={}",
                session.getId(), traceId(session), exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        signalIngestService.unregisterConnection(session.getId());
    }

    private String traceId(WebSocketSession session) {
        Object traceId = session.getAttributes().get(Mt5HandshakeInterceptor.TRACE_ID_ATTR);
        return traceId == null ? session.getId() : traceId.toString();
    }
}
