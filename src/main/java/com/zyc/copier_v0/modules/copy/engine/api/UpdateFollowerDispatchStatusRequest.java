package com.zyc.copier_v0.modules.copy.engine.api;

import com.zyc.copier_v0.modules.copy.engine.domain.FollowerDispatchStatus;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateFollowerDispatchStatusRequest {

    @NotNull
    private FollowerDispatchStatus status;

    @Size(max = 255)
    private String statusMessage;
}
