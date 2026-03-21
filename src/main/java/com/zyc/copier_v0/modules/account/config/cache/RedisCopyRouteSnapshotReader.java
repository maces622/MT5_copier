package com.zyc.copier_v0.modules.account.config.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "copier.account-config.route-cache.backend", havingValue = "redis")
public class RedisCopyRouteSnapshotReader implements CopyRouteSnapshotReader {

    private static final Logger log = LoggerFactory.getLogger(RedisCopyRouteSnapshotReader.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final CopyRouteSnapshotFactory snapshotFactory;
    private final CopyRouteCacheKeyResolver keyResolver;
    private final ObjectMapper objectMapper;

    public RedisCopyRouteSnapshotReader(
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
    public MasterRouteCacheSnapshot loadMasterRoute(Long masterAccountId) {
        MasterRouteCacheSnapshot cached = readJson(keyResolver.masterRouteKey(masterAccountId), MasterRouteCacheSnapshot.class);
        if (cached != null) {
            return cached;
        }

        MasterRouteCacheSnapshot snapshot = snapshotFactory.buildMasterRoute(masterAccountId);
        cacheMasterRoute(snapshot);
        return snapshot;
    }

    @Override
    public FollowerRiskCacheSnapshot loadFollowerRisk(Long followerAccountId) {
        FollowerRiskCacheSnapshot cached = readJson(keyResolver.followerRiskKey(followerAccountId), FollowerRiskCacheSnapshot.class);
        if (cached != null) {
            return cached;
        }

        FollowerRiskCacheSnapshot snapshot = snapshotFactory.buildFollowerRisk(followerAccountId);
        cacheFollowerRisk(followerAccountId, snapshot);
        return snapshot;
    }

    private void cacheMasterRoute(MasterRouteCacheSnapshot snapshot) {
        if (snapshot == null || snapshot.getMasterAccountId() == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    keyResolver.masterRouteKey(snapshot.getMasterAccountId()),
                    writeJson(snapshot)
            );
            stringRedisTemplate.opsForValue().set(
                    keyResolver.masterRouteVersionKey(snapshot.getMasterAccountId()),
                    String.valueOf(snapshot.getRouteVersion())
            );
            for (FollowerRouteCacheItem follower : snapshot.getFollowers()) {
                cacheFollowerRisk(follower.getFollowerAccountId(), follower.getRisk());
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to cache master route snapshot in redis, masterAccountId={}", snapshot.getMasterAccountId(), ex);
        }
    }

    private void cacheFollowerRisk(Long followerAccountId, FollowerRiskCacheSnapshot snapshot) {
        if (followerAccountId == null || snapshot == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    keyResolver.followerRiskKey(followerAccountId),
                    writeJson(snapshot)
            );
        } catch (RuntimeException ex) {
            log.warn("Failed to cache follower risk snapshot in redis, followerAccountId={}", followerAccountId, ex);
        }
    }

    private <T> T readJson(String key, Class<T> targetType) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                return null;
            }
            return objectMapper.readValue(json, targetType);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize route cache entry from redis, key={}", key, ex);
            return null;
        } catch (RuntimeException ex) {
            log.warn("Failed to read route cache entry from redis, key={}", key, ex);
            return null;
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
