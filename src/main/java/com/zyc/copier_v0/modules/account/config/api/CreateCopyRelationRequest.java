package com.zyc.copier_v0.modules.account.config.api;

import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;
import javax.validation.constraints.NotNull;

public class CreateCopyRelationRequest {

    @NotNull
    private Long masterAccountId;

    @NotNull
    private Long followerAccountId;

    @NotNull
    private CopyMode copyMode = CopyMode.BALANCE_RATIO;

    private CopyRelationStatus status = CopyRelationStatus.ACTIVE;

    private Integer priority = 100;

    public Long getMasterAccountId() {
        return masterAccountId;
    }

    public void setMasterAccountId(Long masterAccountId) {
        this.masterAccountId = masterAccountId;
    }

    public Long getFollowerAccountId() {
        return followerAccountId;
    }

    public void setFollowerAccountId(Long followerAccountId) {
        this.followerAccountId = followerAccountId;
    }

    public CopyMode getCopyMode() {
        return copyMode;
    }

    public void setCopyMode(CopyMode copyMode) {
        this.copyMode = copyMode;
    }

    public CopyRelationStatus getStatus() {
        return status;
    }

    public void setStatus(CopyRelationStatus status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
