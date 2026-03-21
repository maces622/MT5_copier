package com.zyc.copier_v0.modules.monitor.service;

import com.zyc.copier_v0.modules.monitor.domain.Mt5ConnectionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class Mt5AccountRuntimeStateSnapshot {

    private Long id;
    private Long accountId;
    private Long login;
    private String server;
    private String accountKey;
    private String lastSessionId;
    private Mt5ConnectionStatus connectionStatus = Mt5ConnectionStatus.UNKNOWN;
    private Instant lastHelloAt;
    private Instant lastHeartbeatAt;
    private Instant lastSignalAt;
    private String lastSignalType;
    private String lastEventId;
    private BigDecimal balance;
    private BigDecimal equity;
    private Instant createdAt;
    private Instant updatedAt;

    public String requireAccountKey() {
        if (!StringUtils.hasText(accountKey)) {
            throw new IllegalStateException("Runtime-state accountKey is required");
        }
        return accountKey;
    }
}
