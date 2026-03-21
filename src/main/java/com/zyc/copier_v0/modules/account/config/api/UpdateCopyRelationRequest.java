package com.zyc.copier_v0.modules.account.config.api;

import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;

public class UpdateCopyRelationRequest {

    private CopyMode copyMode;
    private CopyRelationStatus status;
    private Integer priority;

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
