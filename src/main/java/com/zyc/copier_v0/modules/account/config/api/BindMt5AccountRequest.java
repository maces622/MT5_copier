package com.zyc.copier_v0.modules.account.config.api;

import com.zyc.copier_v0.modules.account.config.domain.AccountStatus;
import com.zyc.copier_v0.modules.account.config.domain.Mt5AccountRole;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class BindMt5AccountRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String brokerName;

    @NotBlank
    private String serverName;

    @NotNull
    private Long mt5Login;

    @NotBlank
    private String credential;

    @NotNull
    private Mt5AccountRole accountRole;

    private AccountStatus status = AccountStatus.ACTIVE;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public Long getMt5Login() {
        return mt5Login;
    }

    public void setMt5Login(Long mt5Login) {
        this.mt5Login = mt5Login;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public Mt5AccountRole getAccountRole() {
        return accountRole;
    }

    public void setAccountRole(Mt5AccountRole accountRole) {
        this.accountRole = accountRole;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
