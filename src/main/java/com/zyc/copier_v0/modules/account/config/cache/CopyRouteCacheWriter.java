package com.zyc.copier_v0.modules.account.config.cache;

import com.zyc.copier_v0.modules.account.config.entity.Mt5AccountEntity;

public interface CopyRouteCacheWriter {

    void refreshMasterRoute(Long masterAccountId);

    void refreshFollowerRisk(Long followerAccountId);

    void refreshAccountBinding(Mt5AccountEntity account);
}
