package com.zyc.copier_v0.modules.account.config.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "copier.account-config.route-cache.backend", havingValue = "redis")
public class RedisCopyRouteCacheWriter implements CopyRouteCacheWriter {

    private static final Logger log = LoggerFactory.getLogger(RedisCopyRouteCacheWriter.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final CopyRouteSnapshotFactory snapshotFactory;
    private final AccountRouteCacheProperties properties;
    private final ObjectMapper objectMapper;

    public RedisCopyRouteCacheWriter(
            StringRedisTemplate stringRedisTemplate,
            CopyRouteSnapshotFactory snapshotFactory,
            AccountRouteCacheProperties properties,
            ObjectMapper objectMapper
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.snapshotFactory = snapshotFactory;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void refreshMasterRoute(Long masterAccountId) {
        MasterRouteCacheSnapshot snapshot = snapshotFactory.buildMasterRoute(masterAccountId);
        stringRedisTemplate.opsForValue().set(masterRouteKey(masterAccountId), writeJson(snapshot));
        stringRedisTemplate.opsForValue().set(masterRouteVersionKey(masterAccountId), String.valueOf(snapshot.getRouteVersion()));
        for (FollowerRouteCacheItem follower : snapshot.getFollowers()) {
            stringRedisTemplate.opsForValue().set(followerRiskKey(follower.getFollowerAccountId()), writeJson(follower.getRisk()));
        }
        log.info("Refresh master route cache in redis, masterAccountId={}, followers={}",
                masterAccountId, snapshot.getFollowers().size());
    }

    @Override
    public void refreshFollowerRisk(Long followerAccountId) {
        FollowerRiskCacheSnapshot snapshot = snapshotFactory.buildFollowerRisk(followerAccountId);
        stringRedisTemplate.opsForValue().set(followerRiskKey(followerAccountId), writeJson(snapshot));
        log.info("Refresh follower risk cache in redis, followerAccountId={}", followerAccountId);
    }

    private String masterRouteKey(Long masterAccountId) {
        return properties.getKeyPrefix() + ":route:master:" + masterAccountId;
    }

    private String masterRouteVersionKey(Long masterAccountId) {
        return properties.getKeyPrefix() + ":route:version:" + masterAccountId;
    }

    private String followerRiskKey(Long followerAccountId) {
        return properties.getKeyPrefix() + ":account:risk:" + followerAccountId;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize route cache payload", ex);
        }
    }
}
