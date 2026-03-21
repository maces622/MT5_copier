package com.zyc.copier_v0.modules.monitor.web;

import com.zyc.copier_v0.modules.account.config.domain.Mt5AccountRole;
import com.zyc.copier_v0.modules.monitor.api.Mt5AccountMonitorOverviewResponse;
import com.zyc.copier_v0.modules.monitor.api.Mt5RuntimeStateResponse;
import com.zyc.copier_v0.modules.monitor.api.Mt5SignalRecordResponse;
import com.zyc.copier_v0.modules.monitor.api.Mt5WsSessionResponse;
import com.zyc.copier_v0.modules.monitor.service.AccountMonitorService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor")
public class AccountMonitorController {

    private final AccountMonitorService accountMonitorService;

    public AccountMonitorController(AccountMonitorService accountMonitorService) {
        this.accountMonitorService = accountMonitorService;
    }

    @GetMapping("/runtime-states")
    public List<Mt5RuntimeStateResponse> listRuntimeStates() {
        return accountMonitorService.listRuntimeStates();
    }

    @GetMapping("/accounts/overview")
    public List<Mt5AccountMonitorOverviewResponse> listAccountOverviews(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Mt5AccountRole accountRole
    ) {
        return accountMonitorService.listAccountOverviews(userId, accountRole);
    }

    @GetMapping("/ws-sessions")
    public List<Mt5WsSessionResponse> listWsSessions(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Mt5AccountRole accountRole
    ) {
        return accountMonitorService.listWsSessions(userId, accountRole);
    }

    @GetMapping("/accounts/{accountId}/signals")
    public List<Mt5SignalRecordResponse> listSignalsByAccountId(@PathVariable Long accountId) {
        return accountMonitorService.listSignalsByAccountId(accountId);
    }

    @GetMapping("/signals")
    public List<Mt5SignalRecordResponse> listSignalsByAccountKey(@RequestParam String accountKey) {
        return accountMonitorService.listSignalsByAccountKey(accountKey);
    }
}
