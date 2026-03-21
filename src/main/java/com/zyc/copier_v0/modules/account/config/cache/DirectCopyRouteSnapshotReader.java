package com.zyc.copier_v0.modules.account.config.cache;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "copier.account-config.route-cache.backend", havingValue = "log", matchIfMissing = true)
public class DirectCopyRouteSnapshotReader implements CopyRouteSnapshotReader {

    private final CopyRouteSnapshotFactory snapshotFactory;
    private final Mt5AccountBindingSnapshotFactory accountBindingSnapshotFactory;

    public DirectCopyRouteSnapshotReader(
            CopyRouteSnapshotFactory snapshotFactory,
            Mt5AccountBindingSnapshotFactory accountBindingSnapshotFactory
    ) {
        this.snapshotFactory = snapshotFactory;
        this.accountBindingSnapshotFactory = accountBindingSnapshotFactory;
    }

    @Override
    public MasterRouteCacheSnapshot loadMasterRoute(Long masterAccountId) {
        return snapshotFactory.buildMasterRoute(masterAccountId);
    }

    @Override
    public FollowerRiskCacheSnapshot loadFollowerRisk(Long followerAccountId) {
        return snapshotFactory.buildFollowerRisk(followerAccountId);
    }

    @Override
    public Optional<Mt5AccountBindingCacheSnapshot> loadAccountBinding(String serverName, Long mt5Login) {
        return accountBindingSnapshotFactory.findByServerAndLogin(serverName, mt5Login);
    }
}
