package com.zyc.copier_v0.modules.account.config.cache;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.account.config.domain.AccountStatus;
import com.zyc.copier_v0.modules.account.config.domain.Mt5AccountRole;
import com.zyc.copier_v0.modules.account.config.entity.Mt5AccountEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RedisCopyRouteCacheWriterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private CopyRouteSnapshotFactory snapshotFactory;

    @Mock
    private Mt5AccountBindingSnapshotFactory accountBindingSnapshotFactory;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisCopyRouteCacheWriter writer;

    @BeforeEach
    void setUp() {
        AccountRouteCacheProperties properties = new AccountRouteCacheProperties();
        properties.setKeyPrefix("copy");
        CopyRouteCacheKeyResolver keyResolver = new CopyRouteCacheKeyResolver(properties);
        writer = new RedisCopyRouteCacheWriter(
                stringRedisTemplate,
                snapshotFactory,
                accountBindingSnapshotFactory,
                keyResolver,
                new ObjectMapper()
        );
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldNotFailConfigWriteWhenRedisIsUnavailable() {
        when(snapshotFactory.buildMasterRoute(1L)).thenReturn(new MasterRouteCacheSnapshot());
        org.mockito.Mockito.doThrow(new IllegalStateException("redis unavailable"))
                .when(valueOperations)
                .set(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        when(accountBindingSnapshotFactory.buildSnapshot(org.mockito.ArgumentMatchers.any(Mt5AccountEntity.class)))
                .thenAnswer(invocation -> {
                    Mt5AccountEntity account = invocation.getArgument(0);
                    Mt5AccountBindingCacheSnapshot snapshot = new Mt5AccountBindingCacheSnapshot();
                    snapshot.setAccountId(account.getId());
                    snapshot.setServerName(account.getServerName());
                    snapshot.setMt5Login(account.getMt5Login());
                    snapshot.setAccountRole(account.getAccountRole());
                    snapshot.setStatus(account.getStatus());
                    return snapshot;
                });

        assertThatCode(() -> writer.refreshMasterRoute(1L)).doesNotThrowAnyException();
        assertThatCode(() -> writer.refreshFollowerRisk(2L)).doesNotThrowAnyException();
        assertThatCode(() -> writer.refreshAccountBinding(account(3L, "Server-A", 10001L))).doesNotThrowAnyException();
    }

    private Mt5AccountEntity account(Long id, String serverName, Long login) {
        Mt5AccountEntity account = new Mt5AccountEntity();
        ReflectionTestUtils.setField(account, "id", id);
        account.setServerName(serverName);
        account.setMt5Login(login);
        account.setAccountRole(Mt5AccountRole.MASTER);
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }
}
