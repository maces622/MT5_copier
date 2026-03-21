package com.zyc.copier_v0.modules.account.config.api;

import com.zyc.copier_v0.modules.account.config.domain.AccountStatus;
import com.zyc.copier_v0.modules.account.config.domain.Mt5AccountRole;
import java.time.Instant;
import lombok.Data;

@Data
public class Mt5AccountResponse {

    private Long id;
    private Long userId;
    private String brokerName;
    private String serverName;
    private Long mt5Login;
    private Integer credentialVersion;
    private boolean credentialConfigured;
    private Mt5AccountRole accountRole;
    private AccountStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
