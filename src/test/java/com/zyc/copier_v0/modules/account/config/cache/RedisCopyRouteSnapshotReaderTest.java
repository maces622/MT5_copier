package com.zyc.copier_v0.modules.account.config.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisCopyRouteSnapshotReaderTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private CopyRouteSnapshotFactory snapshotFactory;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CopyRouteCacheKeyResolver keyResolver;
    private RedisCopyRouteSnapshotReader reader;

    @BeforeEach
    void setUp() {
        AccountRouteCacheProperties properties = new AccountRouteCacheProperties();
        properties.setKeyPrefix("copy");
        keyResolver = new CopyRouteCacheKeyResolver(properties);
        reader = new RedisCopyRouteSnapshotReader(stringRedisTemplate, snapshotFactory, keyResolver, objectMapper);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldLoadMasterRouteFromRedisWithoutDatabaseFallback() throws Exception {
        MasterRouteCacheSnapshot cachedSnapshot = masterRouteSnapshot(1L, 2L);
        when(valueOperations.get(keyResolver.masterRouteKey(1L)))
                .thenReturn(objectMapper.writeValueAsString(cachedSnapshot));

        MasterRouteCacheSnapshot loaded = reader.loadMasterRoute(1L);

        assertThat(loaded.getMasterAccountId()).isEqualTo(1L);
        assertThat(loaded.getFollowers()).hasSize(1);
        assertThat(loaded.getFollowers().get(0).getCopyMode().name()).isEqualTo("BALANCE_RATIO");
        verify(snapshotFactory, never()).buildMasterRoute(1L);
    }

    @Test
    void shouldFallbackToDatabaseAndBackfillRedisOnMasterRouteCacheMiss() {
        MasterRouteCacheSnapshot databaseSnapshot = masterRouteSnapshot(1L, 2L);
        when(valueOperations.get(keyResolver.masterRouteKey(1L))).thenReturn(null);
        when(snapshotFactory.buildMasterRoute(1L)).thenReturn(databaseSnapshot);

        MasterRouteCacheSnapshot loaded = reader.loadMasterRoute(1L);

        assertThat(loaded.getFollowers()).hasSize(1);
        verify(snapshotFactory).buildMasterRoute(1L);
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq(keyResolver.masterRouteKey(1L)),
                org.mockito.ArgumentMatchers.contains("\"masterAccountId\":1")
        );
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq(keyResolver.masterRouteVersionKey(1L)),
                org.mockito.ArgumentMatchers.eq(String.valueOf(databaseSnapshot.getRouteVersion()))
        );
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq(keyResolver.followerRiskKey(2L)),
                org.mockito.ArgumentMatchers.contains("\"accountId\":2")
        );
    }

    @Test
    void shouldFallbackToDatabaseWhenRedisReadFails() {
        MasterRouteCacheSnapshot databaseSnapshot = masterRouteSnapshot(1L, 2L);
        when(valueOperations.get(keyResolver.masterRouteKey(1L)))
                .thenThrow(new IllegalStateException("redis unavailable"));
        when(snapshotFactory.buildMasterRoute(1L)).thenReturn(databaseSnapshot);

        MasterRouteCacheSnapshot loaded = reader.loadMasterRoute(1L);

        assertThat(loaded.getMasterAccountId()).isEqualTo(1L);
        verify(snapshotFactory).buildMasterRoute(1L);
    }

    private MasterRouteCacheSnapshot masterRouteSnapshot(Long masterAccountId, Long followerAccountId) {
        FollowerRiskCacheSnapshot risk = new FollowerRiskCacheSnapshot();
        risk.setAccountId(followerAccountId);
        risk.setBalanceRatio(new BigDecimal("1.0"));
        risk.setMaxLot(new BigDecimal("1.0"));

        FollowerRouteCacheItem follower = new FollowerRouteCacheItem();
        follower.setFollowerAccountId(followerAccountId);
        follower.setCopyMode(com.zyc.copier_v0.modules.account.config.domain.CopyMode.BALANCE_RATIO);
        follower.setPriority(100);
        follower.setConfigVersion(2L);
        follower.setRisk(risk);

        MasterRouteCacheSnapshot snapshot = new MasterRouteCacheSnapshot();
        snapshot.setMasterAccountId(masterAccountId);
        snapshot.setRouteVersion(2L);
        snapshot.setFollowers(List.of(follower));
        return snapshot;
    }
}
