package com.zyc.copier_v0.modules.account.config.cache;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class MasterRouteCacheSnapshot {

    private Long masterAccountId;
    private Long routeVersion;
    private List<FollowerRouteCacheItem> followers = new ArrayList<>();
}
