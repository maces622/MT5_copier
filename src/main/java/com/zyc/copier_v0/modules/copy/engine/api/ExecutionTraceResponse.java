package com.zyc.copier_v0.modules.copy.engine.api;

import java.util.ArrayList;
import java.util.List;

public class ExecutionTraceResponse {

    private Long masterAccountId;
    private Long masterOrderId;
    private Long masterPositionId;
    private List<ExecutionCommandResponse> commands = new ArrayList<>();
    private List<FollowerDispatchOutboxResponse> dispatches = new ArrayList<>();

    public Long getMasterAccountId() {
        return masterAccountId;
    }

    public void setMasterAccountId(Long masterAccountId) {
        this.masterAccountId = masterAccountId;
    }

    public Long getMasterOrderId() {
        return masterOrderId;
    }

    public void setMasterOrderId(Long masterOrderId) {
        this.masterOrderId = masterOrderId;
    }

    public Long getMasterPositionId() {
        return masterPositionId;
    }

    public void setMasterPositionId(Long masterPositionId) {
        this.masterPositionId = masterPositionId;
    }

    public List<ExecutionCommandResponse> getCommands() {
        return commands;
    }

    public void setCommands(List<ExecutionCommandResponse> commands) {
        this.commands = commands;
    }

    public List<FollowerDispatchOutboxResponse> getDispatches() {
        return dispatches;
    }

    public void setDispatches(List<FollowerDispatchOutboxResponse> dispatches) {
        this.dispatches = dispatches;
    }
}
