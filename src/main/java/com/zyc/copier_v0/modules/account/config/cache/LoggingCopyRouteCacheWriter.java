package com.zyc.copier_v0.modules.account.config.cache;

import com.zyc.copier_v0.modules.account.config.entity.Mt5AccountEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "copier.account-config.route-cache.backend", havingValue = "log", matchIfMissing = true)
public class LoggingCopyRouteCacheWriter implements CopyRouteCacheWriter {

    private static final Logger log = LoggerFactory.getLogger(LoggingCopyRouteCacheWriter.class);

    private final CopyRouteSnapshotFactory snapshotFactory;

    public LoggingCopyRouteCacheWriter(CopyRouteSnapshotFactory snapshotFactory) {
        this.snapshotFactory = snapshotFactory;
    }

    @Override
    public void refreshMasterRoute(Long masterAccountId) {
        MasterRouteCacheSnapshot snapshot = snapshotFactory.buildMasterRoute(masterAccountId);
        log.info("Refresh master route cache via log backend, masterAccountId={}, followers={}, routeVersion={}",
                masterAccountId, snapshot.getFollowers().size(), snapshot.getRouteVersion());
    }

    @Override
    public void refreshFollowerRisk(Long followerAccountId) {
        FollowerRiskCacheSnapshot snapshot = snapshotFactory.buildFollowerRisk(followerAccountId);
        log.info("Refresh follower risk cache via log backend, followerAccountId={}, hasRisk={}",
                followerAccountId,
                snapshot.getMaxLot() != null
                        || snapshot.getFixedLot() != null
                        || snapshot.getBalanceRatio() != null);
    }

    @Override
    public void refreshAccountBinding(Mt5AccountEntity account) {
        if (account == null || account.getId() == null) {
            return;
        }
        log.info("Refresh account binding cache via log backend, accountId={}, accountKey={}:{}",
                account.getId(), account.getServerName(), account.getMt5Login());
    }
}
