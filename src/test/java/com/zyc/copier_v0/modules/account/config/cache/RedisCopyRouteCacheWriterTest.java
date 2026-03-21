package com.zyc.copier_v0.modules.account.config.cache;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisCopyRouteCacheWriterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private CopyRouteSnapshotFactory snapshotFactory;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisCopyRouteCacheWriter writer;

    @BeforeEach
    void setUp() {
        AccountRouteCacheProperties properties = new AccountRouteCacheProperties();
        properties.setKeyPrefix("copy");
        CopyRouteCacheKeyResolver keyResolver = new CopyRouteCacheKeyResolver(properties);
        writer = new RedisCopyRouteCacheWriter(stringRedisTemplate, snapshotFactory, keyResolver, new ObjectMapper());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldNotFailConfigWriteWhenRedisIsUnavailable() {
        when(snapshotFactory.buildMasterRoute(1L)).thenReturn(new MasterRouteCacheSnapshot());
        org.mockito.Mockito.doThrow(new IllegalStateException("redis unavailable"))
                .when(valueOperations)
                .set(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

        assertThatCode(() -> writer.refreshMasterRoute(1L)).doesNotThrowAnyException();
        assertThatCode(() -> writer.refreshFollowerRisk(2L)).doesNotThrowAnyException();
    }
}
