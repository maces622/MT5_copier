package com.zyc.copier_v0.modules.account.config.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "copier.account-config.route-cache.backend", havingValue = "log", matchIfMissing = true)
public class DirectCopyRouteSnapshotReader implements CopyRouteSnapshotReader {

    private final CopyRouteSnapshotFactory snapshotFactory;

    public DirectCopyRouteSnapshotReader(CopyRouteSnapshotFactory snapshotFactory) {
        this.snapshotFactory = snapshotFactory;
    }

    @Override
    public MasterRouteCacheSnapshot loadMasterRoute(Long masterAccountId) {
        return snapshotFactory.buildMasterRoute(masterAccountId);
    }

    @Override
    public FollowerRiskCacheSnapshot loadFollowerRisk(Long followerAccountId) {
        return snapshotFactory.buildFollowerRisk(followerAccountId);
    }
}
