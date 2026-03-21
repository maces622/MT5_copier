package com.zyc.copier_v0.modules.monitor.api;

import com.zyc.copier_v0.modules.account.config.domain.AccountStatus;
import com.zyc.copier_v0.modules.account.config.domain.Mt5AccountRole;
import com.zyc.copier_v0.modules.monitor.domain.Mt5ConnectionStatus;
import java.time.Instant;
import lombok.Data;

@Data
public class Mt5AccountMonitorOverviewResponse {

    private Long accountId;
    private Long userId;
    private String brokerName;
    private String serverName;
    private Long mt5Login;
    private String accountKey;
    private Mt5AccountRole accountRole;
    private AccountStatus accountStatus;
    private Mt5ConnectionStatus connectionStatus;
    private Instant lastHelloAt;
    private Instant lastHeartbeatAt;
    private Instant lastSignalAt;
    private String lastSignalType;
    private String lastEventId;
    private Long activeFollowerCount;
    private Long activeMasterCount;
    private Long pendingDispatchCount;
    private Long failedDispatchCount;
    private Instant updatedAt;
}
