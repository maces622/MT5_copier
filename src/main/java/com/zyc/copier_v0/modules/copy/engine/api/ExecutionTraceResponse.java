package com.zyc.copier_v0.modules.copy.engine.api;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ExecutionTraceResponse {

    private Long masterAccountId;
    private Long masterOrderId;
    private Long masterPositionId;
    private List<ExecutionCommandResponse> commands = new ArrayList<>();
    private List<FollowerDispatchOutboxResponse> dispatches = new ArrayList<>();
}
