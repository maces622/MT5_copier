package com.zyc.copier_v0.modules.account.config.cache;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zyc.copier_v0.modules.account.config.domain.AccountStatus;
import com.zyc.copier_v0.modules.account.config.domain.Mt5AccountRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Mt5AccountBindingCacheSnapshot {

    private Long accountId;
    private Long userId;
    private String brokerName;
    private String serverName;
    private Long mt5Login;
    private Mt5AccountRole accountRole;
    private AccountStatus status;

    @JsonIgnore
    public boolean isBound() {
        return accountId != null;
    }

    public static Mt5AccountBindingCacheSnapshot missing(String serverName, Long mt5Login) {
        Mt5AccountBindingCacheSnapshot snapshot = new Mt5AccountBindingCacheSnapshot();
        snapshot.setServerName(serverName);
        snapshot.setMt5Login(mt5Login);
        return snapshot;
    }
}
