package com.zyc.copier_v0.modules.signal.ingest.event;

import com.zyc.copier_v0.modules.signal.ingest.domain.NormalizedMt5Signal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Mt5SignalAcceptedEvent {

    private final NormalizedMt5Signal signal;
}
