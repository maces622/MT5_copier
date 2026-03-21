package com.zyc.copier_v0.modules.signal.ingest.event;

import com.zyc.copier_v0.modules.signal.ingest.domain.Mt5SessionContext;
import java.time.Instant;

public class Mt5SessionDisconnectedEvent {

    private final Mt5SessionContext sessionContext;
    private final Instant disconnectedAt;

    public Mt5SessionDisconnectedEvent(Mt5SessionContext sessionContext, Instant disconnectedAt) {
        this.sessionContext = sessionContext;
        this.disconnectedAt = disconnectedAt;
    }

    public Mt5SessionContext getSessionContext() {
        return sessionContext;
    }

    public Instant getDisconnectedAt() {
        return disconnectedAt;
    }
}
