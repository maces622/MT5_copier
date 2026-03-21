package com.zyc.copier_v0.modules.account.config.cache;

public interface CopyRouteCacheWriter {

    void refreshMasterRoute(Long masterAccountId);

    void refreshFollowerRisk(Long followerAccountId);
}
