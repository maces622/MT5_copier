package com.zyc.copier_v0.modules.copy.engine.api;

import com.zyc.copier_v0.modules.copy.engine.domain.FollowerDispatchStatus;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class UpdateFollowerDispatchStatusRequest {

    @NotNull
    private FollowerDispatchStatus status;

    @Size(max = 255)
    private String statusMessage;

    public FollowerDispatchStatus getStatus() {
        return status;
    }

    public void setStatus(FollowerDispatchStatus status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
