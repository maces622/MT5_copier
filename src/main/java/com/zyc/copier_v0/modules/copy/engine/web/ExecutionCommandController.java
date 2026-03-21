package com.zyc.copier_v0.modules.copy.engine.web;

import com.zyc.copier_v0.modules.copy.engine.api.ExecutionCommandResponse;
import com.zyc.copier_v0.modules.copy.engine.api.ExecutionTraceResponse;
import com.zyc.copier_v0.modules.copy.engine.api.FollowerDispatchOutboxResponse;
import com.zyc.copier_v0.modules.copy.engine.api.UpdateFollowerDispatchStatusRequest;
import com.zyc.copier_v0.modules.copy.engine.domain.FollowerDispatchStatus;
import com.zyc.copier_v0.modules.copy.engine.service.CopyEngineService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/execution-commands")
public class ExecutionCommandController {

    private final CopyEngineService copyEngineService;

    public ExecutionCommandController(CopyEngineService copyEngineService) {
        this.copyEngineService = copyEngineService;
    }

    @GetMapping
    public List<ExecutionCommandResponse> queryByMasterEventId(@RequestParam(required = false) String masterEventId) {
        return copyEngineService.findByMasterEventId(masterEventId);
    }

    @GetMapping("/followers/{followerAccountId}")
    public List<ExecutionCommandResponse> queryByFollower(@PathVariable Long followerAccountId) {
        return copyEngineService.findByFollowerAccountId(followerAccountId);
    }

    @GetMapping("/order-trace")
    public ExecutionTraceResponse queryOrderTrace(
            @RequestParam Long masterAccountId,
            @RequestParam Long masterOrderId
    ) {
        return copyEngineService.findOrderTrace(masterAccountId, masterOrderId);
    }

    @GetMapping("/position-trace")
    public ExecutionTraceResponse queryPositionTrace(
            @RequestParam Long masterAccountId,
            @RequestParam Long masterPositionId
    ) {
        return copyEngineService.findPositionTrace(masterAccountId, masterPositionId);
    }

    @GetMapping("/dispatches")
    public List<FollowerDispatchOutboxResponse> queryDispatchByMasterEventId(@RequestParam(required = false) String masterEventId) {
        return copyEngineService.findDispatchesByMasterEventId(masterEventId);
    }

    @GetMapping("/dispatches/followers/{followerAccountId}")
    public List<FollowerDispatchOutboxResponse> queryDispatchByFollower(
            @PathVariable Long followerAccountId,
            @RequestParam(required = false) FollowerDispatchStatus status
    ) {
        return copyEngineService.findDispatchesByFollower(followerAccountId, status);
    }

    @PatchMapping("/dispatches/{dispatchId}")
    public FollowerDispatchOutboxResponse updateDispatchStatus(
            @PathVariable Long dispatchId,
            @Valid @RequestBody UpdateFollowerDispatchStatusRequest request
    ) {
        return copyEngineService.updateDispatchStatus(dispatchId, request.getStatus(), request.getStatusMessage());
    }
}
