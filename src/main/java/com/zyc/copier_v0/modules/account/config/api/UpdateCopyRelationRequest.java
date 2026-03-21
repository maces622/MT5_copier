package com.zyc.copier_v0.modules.account.config.api;

import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;
import lombok.Data;

@Data
public class UpdateCopyRelationRequest {

    private CopyMode copyMode;
    private CopyRelationStatus status;
    private Integer priority;
}
