package com.zyc.copier_v0.modules.account.config.cache;

public interface CopyRouteSnapshotReader {

    MasterRouteCacheSnapshot loadMasterRoute(Long masterAccountId);

    FollowerRiskCacheSnapshot loadFollowerRisk(Long followerAccountId);
}
