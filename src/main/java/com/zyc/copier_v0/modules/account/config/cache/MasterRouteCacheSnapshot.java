package com.zyc.copier_v0.modules.account.config.cache;

import java.util.ArrayList;
import java.util.List;

public class MasterRouteCacheSnapshot {

    private Long masterAccountId;
    private Long routeVersion;
    private List<FollowerRouteCacheItem> followers = new ArrayList<>();

    public Long getMasterAccountId() {
        return masterAccountId;
    }

    public void setMasterAccountId(Long masterAccountId) {
        this.masterAccountId = masterAccountId;
    }

    public Long getRouteVersion() {
        return routeVersion;
    }

    public void setRouteVersion(Long routeVersion) {
        this.routeVersion = routeVersion;
    }

    public List<FollowerRouteCacheItem> getFollowers() {
        return followers;
    }

    public void setFollowers(List<FollowerRouteCacheItem> followers) {
        this.followers = followers;
    }
}
