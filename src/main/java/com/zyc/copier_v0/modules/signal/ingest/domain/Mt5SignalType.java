package com.zyc.copier_v0.modules.signal.ingest.domain;

import java.util.Locale;

public enum Mt5SignalType {
    HELLO,
    HEARTBEAT,
    DEAL,
    ORDER;

    public static Mt5SignalType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Signal type is required");
        }
        try {
            return Mt5SignalType.valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported signal type: " + code, ex);
        }
    }

    public boolean requiresEventId() {
        return this == DEAL || this == ORDER;
    }

    public boolean shouldDeduplicate() {
        return this == DEAL || this == ORDER;
    }
}
