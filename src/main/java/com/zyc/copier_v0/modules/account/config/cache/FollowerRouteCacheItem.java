package com.zyc.copier_v0.modules.account.config.cache;

import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import lombok.Data;

@Data
public class FollowerRouteCacheItem {

    private Long followerAccountId;
    private CopyMode copyMode;
    private Integer priority;
    private Long configVersion;
    private FollowerRiskCacheSnapshot risk;
}
