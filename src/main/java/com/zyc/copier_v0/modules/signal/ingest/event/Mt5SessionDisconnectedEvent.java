package com.zyc.copier_v0.modules.signal.ingest.event;

import com.zyc.copier_v0.modules.signal.ingest.domain.Mt5SessionContext;
import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Mt5SessionDisconnectedEvent {

    private final Mt5SessionContext sessionContext;
    private final Instant disconnectedAt;
}
