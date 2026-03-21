package com.zyc.copier_v0.modules.account.config.api;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class SaveSymbolMappingRequest {

    @NotNull
    private Long followerAccountId;

    @NotBlank
    private String masterSymbol;

    @NotBlank
    private String followerSymbol;

    public Long getFollowerAccountId() {
        return followerAccountId;
    }

    public void setFollowerAccountId(Long followerAccountId) {
        this.followerAccountId = followerAccountId;
    }

    public String getMasterSymbol() {
        return masterSymbol;
    }

    public void setMasterSymbol(String masterSymbol) {
        this.masterSymbol = masterSymbol;
    }

    public String getFollowerSymbol() {
        return followerSymbol;
    }

    public void setFollowerSymbol(String followerSymbol) {
        this.followerSymbol = followerSymbol;
    }
}
