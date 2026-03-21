package com.zyc.copier_v0.modules.signal.ingest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.signal.ingest.event.Mt5SignalAcceptedEvent;
import com.zyc.copier_v0.modules.signal.ingest.event.Mt5SessionDisconnectedEvent;
import com.zyc.copier_v0.modules.signal.ingest.domain.Mt5SignalType;
import com.zyc.copier_v0.modules.signal.ingest.domain.NormalizedMt5Signal;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class Mt5SignalIngestService {

    private static final Logger log = LoggerFactory.getLogger(Mt5SignalIngestService.class);

    private final ObjectMapper objectMapper;
    private final Mt5SignalNormalizer signalNormalizer;
    private final Mt5SignalDedupService signalDedupService;
    private final Mt5SessionRegistry sessionRegistry;
    private final Mt5SignalPublisher signalPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;

    public Mt5SignalIngestService(
            ObjectMapper objectMapper,
            Mt5SignalNormalizer signalNormalizer,
            Mt5SignalDedupService signalDedupService,
            Mt5SessionRegistry sessionRegistry,
            Mt5SignalPublisher signalPublisher,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.objectMapper = objectMapper;
        this.signalNormalizer = signalNormalizer;
        this.signalDedupService = signalDedupService;
        this.sessionRegistry = sessionRegistry;
        this.signalPublisher = signalPublisher;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void registerConnection(String sessionId, String traceId) {
        sessionRegistry.register(sessionId, traceId);
        log.info("MT5 websocket connected, sessionId={}, traceId={}", sessionId, traceId);
    }

    public void unregisterConnection(String sessionId) {
        sessionRegistry.remove(sessionId).ifPresent(sessionContext ->
                applicationEventPublisher.publishEvent(new Mt5SessionDisconnectedEvent(sessionContext, Instant.now()))
        );
        log.info("MT5 websocket disconnected, sessionId={}", sessionId);
    }

    public void ingest(String sessionId, String traceId, String textPayload) throws JsonProcessingException {
        JsonNode payload = objectMapper.readTree(textPayload);
        NormalizedMt5Signal signal = signalNormalizer.normalize(
                payload,
                sessionId,
                traceId,
                Instant.now(),
                sessionRegistry.get(sessionId).orElse(null)
        );

        if (signal.getType() == Mt5SignalType.HELLO
                && signal.getLogin() != null
                && StringUtils.hasText(signal.getServer())) {
            sessionRegistry.bindAccount(sessionId, signal.getLogin(), signal.getServer());
        }

        sessionRegistry.touch(sessionId);

        if (signal.getType().shouldDeduplicate() && !signalDedupService.markIfNew(signal.getEventId())) {
            log.info("Skip duplicate MT5 signal, eventId={}, sessionId={}", signal.getEventId(), sessionId);
            return;
        }

        signalPublisher.publish(signal);
        applicationEventPublisher.publishEvent(new Mt5SignalAcceptedEvent(signal));
    }
}
