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
    private final CopyRouteCacheKeyResolver keyResolver;
    private final ObjectMapper objectMapper;

    public RedisCopyRouteCacheWriter(
            StringRedisTemplate stringRedisTemplate,
            CopyRouteSnapshotFactory snapshotFactory,
            CopyRouteCacheKeyResolver keyResolver,
            ObjectMapper objectMapper
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.snapshotFactory = snapshotFactory;
        this.keyResolver = keyResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public void refreshMasterRoute(Long masterAccountId) {
        MasterRouteCacheSnapshot snapshot = snapshotFactory.buildMasterRoute(masterAccountId);
        try {
            stringRedisTemplate.opsForValue().set(keyResolver.masterRouteKey(masterAccountId), writeJson(snapshot));
            stringRedisTemplate.opsForValue().set(
                    keyResolver.masterRouteVersionKey(masterAccountId),
                    String.valueOf(snapshot.getRouteVersion())
            );
            for (FollowerRouteCacheItem follower : snapshot.getFollowers()) {
                stringRedisTemplate.opsForValue().set(
                        keyResolver.followerRiskKey(follower.getFollowerAccountId()),
                        writeJson(follower.getRisk())
                );
            }
            log.info("Refresh master route cache in redis, masterAccountId={}, followers={}",
                    masterAccountId, snapshot.getFollowers().size());
        } catch (RuntimeException ex) {
            log.warn("Failed to refresh master route cache in redis, masterAccountId={}", masterAccountId, ex);
        }
    }

    @Override
    public void refreshFollowerRisk(Long followerAccountId) {
        FollowerRiskCacheSnapshot snapshot = snapshotFactory.buildFollowerRisk(followerAccountId);
        try {
            stringRedisTemplate.opsForValue().set(keyResolver.followerRiskKey(followerAccountId), writeJson(snapshot));
            log.info("Refresh follower risk cache in redis, followerAccountId={}", followerAccountId);
        } catch (RuntimeException ex) {
            log.warn("Failed to refresh follower risk cache in redis, followerAccountId={}", followerAccountId, ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize route cache payload", ex);
        }
    }
}
