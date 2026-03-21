package com.zyc.copier_v0.modules.account.config.cache;

import org.springframework.stereotype.Component;

@Component
public class CopyRouteCacheKeyResolver {

    private final AccountRouteCacheProperties properties;

    public CopyRouteCacheKeyResolver(AccountRouteCacheProperties properties) {
        this.properties = properties;
    }

    public String masterRouteKey(Long masterAccountId) {
        return properties.getKeyPrefix() + ":route:master:" + masterAccountId;
    }

    public String masterRouteVersionKey(Long masterAccountId) {
        return properties.getKeyPrefix() + ":route:version:" + masterAccountId;
    }

    public String followerRiskKey(Long followerAccountId) {
        return properties.getKeyPrefix() + ":account:risk:" + followerAccountId;
    }

    public String accountBindingKey(String serverName, Long mt5Login) {
        return properties.getKeyPrefix() + ":account:binding:" + serverName + ":" + mt5Login;
    }
}
