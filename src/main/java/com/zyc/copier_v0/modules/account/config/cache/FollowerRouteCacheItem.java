package com.zyc.copier_v0.modules.account.config.cache;

import com.zyc.copier_v0.modules.account.config.domain.CopyMode;

public class FollowerRouteCacheItem {

    private Long followerAccountId;
    private CopyMode copyMode;
    private Integer priority;
    private Long configVersion;
    private FollowerRiskCacheSnapshot risk;

    public Long getFollowerAccountId() {
        return followerAccountId;
    }

    public void setFollowerAccountId(Long followerAccountId) {
        this.followerAccountId = followerAccountId;
    }

    public CopyMode getCopyMode() {
        return copyMode;
    }

    public void setCopyMode(CopyMode copyMode) {
        this.copyMode = copyMode;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Long getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(Long configVersion) {
        this.configVersion = configVersion;
    }

    public FollowerRiskCacheSnapshot getRisk() {
        return risk;
    }

    public void setRisk(FollowerRiskCacheSnapshot risk) {
        this.risk = risk;
    }
}
