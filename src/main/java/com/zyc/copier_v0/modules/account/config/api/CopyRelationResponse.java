package com.zyc.copier_v0.modules.account.config.api;

import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;
import java.time.Instant;
import lombok.Data;

@Data
public class CopyRelationResponse {

    private Long id;
    private Long masterAccountId;
    private Long followerAccountId;
    private CopyMode copyMode;
    private CopyRelationStatus status;
    private Integer priority;
    private Long configVersion;
    private Instant createdAt;
    private Instant updatedAt;
}
