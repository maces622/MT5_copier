package com.zyc.copier_v0.modules.account.config.cache;

import java.util.Optional;

public interface CopyRouteSnapshotReader {

    MasterRouteCacheSnapshot loadMasterRoute(Long masterAccountId);

    FollowerRiskCacheSnapshot loadFollowerRisk(Long followerAccountId);

    Optional<Mt5AccountBindingCacheSnapshot> loadAccountBinding(String serverName, Long mt5Login);
}
