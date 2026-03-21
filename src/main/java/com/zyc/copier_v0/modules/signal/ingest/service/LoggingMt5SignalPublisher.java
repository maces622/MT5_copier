package com.zyc.copier_v0.modules.signal.ingest.service;

import com.zyc.copier_v0.modules.signal.ingest.domain.Mt5SignalType;
import com.zyc.copier_v0.modules.signal.ingest.domain.NormalizedMt5Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingMt5SignalPublisher implements Mt5SignalPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingMt5SignalPublisher.class);

    @Override
    public void publish(NormalizedMt5Signal signal) {
        if (signal.getType() == Mt5SignalType.HEARTBEAT) {
            log.debug("Accepted MT5 heartbeat, traceId={}, sessionId={}, account={}",
                    signal.getTraceId(), signal.getSessionId(), signal.getMasterAccountKey());
            return;
        }

        log.info("Accepted MT5 signal, type={}, eventId={}, account={}, traceId={}",
                signal.getType(), signal.getEventId(), signal.getMasterAccountKey(), signal.getTraceId());
    }
}
