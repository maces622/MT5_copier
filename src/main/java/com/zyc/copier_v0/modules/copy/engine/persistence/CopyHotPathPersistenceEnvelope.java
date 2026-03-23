package com.zyc.copier_v0.modules.copy.engine.persistence;

import java.time.Instant;
import lombok.Data;

@Data
public class CopyHotPathPersistenceEnvelope {

    private CopyHotPathPersistenceMessageType type;
    private String payloadJson;
    private Instant createdAt = Instant.now();
}
