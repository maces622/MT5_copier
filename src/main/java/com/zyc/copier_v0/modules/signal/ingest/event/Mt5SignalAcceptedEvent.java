package com.zyc.copier_v0.modules.signal.ingest.event;

import com.zyc.copier_v0.modules.signal.ingest.domain.NormalizedMt5Signal;

public class Mt5SignalAcceptedEvent {

    private final NormalizedMt5Signal signal;

    public Mt5SignalAcceptedEvent(NormalizedMt5Signal signal) {
        this.signal = signal;
    }

    public NormalizedMt5Signal getSignal() {
        return signal;
    }
}
