package com.zyc.copier_v0.modules.account.config.api;

import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCopyRelationRequest {

    @NotNull
    private Long masterAccountId;

    @NotNull
    private Long followerAccountId;

    @NotNull
    private CopyMode copyMode = CopyMode.BALANCE_RATIO;

    private CopyRelationStatus status = CopyRelationStatus.ACTIVE;

    private Integer priority = 100;
}
