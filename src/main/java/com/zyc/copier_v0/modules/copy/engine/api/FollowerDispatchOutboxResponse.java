package com.zyc.copier_v0.modules.copy.engine.api;

import com.zyc.copier_v0.modules.copy.engine.domain.FollowerDispatchStatus;
import java.time.Instant;
import lombok.Data;

@Data
public class FollowerDispatchOutboxResponse {

    private Long id;
    private Long executionCommandId;
    private String masterEventId;
    private Long followerAccountId;
    private FollowerDispatchStatus status;
    private String statusMessage;
    private String payloadJson;
    private Instant ackedAt;
    private Instant failedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
