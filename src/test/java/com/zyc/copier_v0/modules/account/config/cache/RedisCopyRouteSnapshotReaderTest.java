package com.zyc.copier_v0.modules.account.config.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.account.config.domain.AccountStatus;
import com.zyc.copier_v0.modules.account.config.domain.Mt5AccountRole;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
    private Mt5AccountBindingSnapshotFactory accountBindingSnapshotFactory;

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
        reader = new RedisCopyRouteSnapshotReader(
                stringRedisTemplate,
                snapshotFactory,
                accountBindingSnapshotFactory,
                keyResolver,
                objectMapper
        );
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

    @Test
    void shouldLoadAccountBindingFromRedisWithoutDatabaseFallback() throws Exception {
        Mt5AccountBindingCacheSnapshot cached = accountBindingSnapshot(1L, "Server-A", 10001L);
        when(valueOperations.get(keyResolver.accountBindingKey("Server-A", 10001L)))
                .thenReturn(objectMapper.writeValueAsString(cached));

        Optional<Mt5AccountBindingCacheSnapshot> loaded = reader.loadAccountBinding("Server-A", 10001L);

        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().getAccountId()).isEqualTo(1L);
        verify(accountBindingSnapshotFactory, never()).findByServerAndLogin("Server-A", 10001L);
    }

    @Test
    void shouldFallbackToDatabaseAndBackfillRedisOnAccountBindingCacheMiss() {
        Mt5AccountBindingCacheSnapshot databaseSnapshot = accountBindingSnapshot(1L, "Server-A", 10001L);
        when(valueOperations.get(keyResolver.accountBindingKey("Server-A", 10001L))).thenReturn(null);
        when(accountBindingSnapshotFactory.findByServerAndLogin("Server-A", 10001L))
                .thenReturn(Optional.of(databaseSnapshot));

        Optional<Mt5AccountBindingCacheSnapshot> loaded = reader.loadAccountBinding("Server-A", 10001L);

        assertThat(loaded).isPresent();
        verify(accountBindingSnapshotFactory).findByServerAndLogin("Server-A", 10001L);
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq(keyResolver.accountBindingKey("Server-A", 10001L)),
                org.mockito.ArgumentMatchers.contains("\"accountId\":1")
        );
    }

    @Test
    void shouldNegativeCacheMissingAccountBinding() {
        when(valueOperations.get(keyResolver.accountBindingKey("Unknown-Server", 99999L))).thenReturn(null);
        when(accountBindingSnapshotFactory.findByServerAndLogin("Unknown-Server", 99999L)).thenReturn(Optional.empty());

        Optional<Mt5AccountBindingCacheSnapshot> loaded = reader.loadAccountBinding("Unknown-Server", 99999L);

        assertThat(loaded).isEmpty();
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq(keyResolver.accountBindingKey("Unknown-Server", 99999L)),
                org.mockito.ArgumentMatchers.contains("\"serverName\":\"Unknown-Server\"")
        );
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

    private Mt5AccountBindingCacheSnapshot accountBindingSnapshot(Long accountId, String serverName, Long login) {
        Mt5AccountBindingCacheSnapshot snapshot = new Mt5AccountBindingCacheSnapshot();
        snapshot.setAccountId(accountId);
        snapshot.setServerName(serverName);
        snapshot.setMt5Login(login);
        snapshot.setAccountRole(Mt5AccountRole.MASTER);
        snapshot.setStatus(AccountStatus.ACTIVE);
        return snapshot;
    }
}
