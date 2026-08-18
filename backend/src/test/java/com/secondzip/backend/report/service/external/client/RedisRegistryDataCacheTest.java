package com.secondzip.backend.report.service.external.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.report.dto.external.RegistryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRegistryDataCacheTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private RedisRegistryDataCache cache;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cache = new RedisRegistryDataCache(redisTemplate, objectMapper);
    }

    @Test
    void findDeserializesRegistryDataUsingPrefixedKey() throws Exception {
        RegistryData source = registryData();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("registry:listing-1"))
                .thenReturn(objectMapper.writeValueAsString(source));

        RegistryData result = cache.find("listing-1");

        assertThat(result.getMortgageAmount()).isEqualTo(120_000_000L);
        assertThat(result.getHasSeizure()).isFalse();
        assertThat(result.getOwnerName()).isEqualTo("홍길동");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void blankCachedValueIsAMiss(String json) {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("registry:key")).thenReturn(json);

        assertThat(cache.find("key")).isNull();
    }

    @Test
    void nullKeyOrUnavailableRedisIsAMiss() {
        assertThat(cache.find(null)).isNull();
        assertThat(new RedisRegistryDataCache(null, objectMapper).find("key")).isNull();

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void malformedJsonOrRedisFailureIsAMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("registry:bad-json")).thenReturn("{");
        when(valueOperations.get("registry:redis-down")).thenThrow(new IllegalStateException("down"));

        assertThat(cache.find("bad-json")).isNull();
        assertThat(cache.find("redis-down")).isNull();
    }

    @Test
    void putSerializesDataWithConfiguredTtlAndPrefix() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(cache, "ttlSeconds", 300L);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        cache.put("listing-1", registryData());

        verify(valueOperations).set(
                eq("registry:listing-1"),
                jsonCaptor.capture(),
                eq(Duration.ofSeconds(300))
        );
        assertThat(jsonCaptor.getValue()).contains("120000000", "홍길동");
    }

    @Test
    void nonPositiveConfiguredTtlIsClampedToOneSecond() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(cache, "ttlSeconds", 0L);

        cache.put("key", registryData());

        verify(valueOperations).set(anyString(), anyString(), eq(Duration.ofSeconds(1)));
    }

    @Test
    void nullPutArgumentsAreIgnored() {
        cache.put(null, registryData());
        cache.put("key", null);
        new RedisRegistryDataCache(null, objectMapper).put("key", registryData());

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void redisWriteFailureDoesNotFailPaidLookupResult() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new IllegalStateException("down"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        assertThatCode(() -> cache.put("key", registryData())).doesNotThrowAnyException();
        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }

    private RegistryData registryData() {
        RegistryData data = new RegistryData();
        data.setMortgageAmount(120_000_000L);
        data.setHasSeizure(false);
        data.setHasTrustRegistration(false);
        data.setOwnerName("홍길동");
        data.setOwnerType("INDIVIDUAL");
        data.setLandOwnerName("홍길동");
        data.setHasPostTrustInfringement(false);
        return data;
    }
}
