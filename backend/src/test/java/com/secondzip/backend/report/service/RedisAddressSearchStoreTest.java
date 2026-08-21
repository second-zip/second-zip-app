package com.secondzip.backend.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AnalysisTargetDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAddressSearchStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("주소 후보를 UUID 키의 JSON으로 설정 TTL 동안 저장한다")
    void savesTargetAsJsonWithConfiguredTtl() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ObjectMapper objectMapper = new ObjectMapper();
        RedisAddressSearchStore store = store(objectMapper, 1_800L);
        AnalysisTargetDTO target = target();

        String addressId = store.save(target);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(
                keyCaptor.capture(),
                jsonCaptor.capture(),
                ttlCaptor.capture()
        );
        assertThat(keyCaptor.getValue())
                .isEqualTo("address-search:" + addressId);
        assertThat(addressId).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        );
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofSeconds(1_800L));
        assertThat(objectMapper.readValue(
                jsonCaptor.getValue(),
                AnalysisTargetDTO.class
        )).isEqualTo(target);
    }

    @Test
    @DisplayName("설정 TTL이 너무 짧아도 주소 후보를 최소 60초 보관한다")
    void enforcesMinimumTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RedisAddressSearchStore store = store(new ObjectMapper(), 59L);

        store.save(target());

        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.startsWith("address-search:"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(Duration.ofSeconds(60L))
        );
    }

    @Test
    @DisplayName("주소 JSON 직렬화 실패를 내부 서버 오류로 변환하고 Redis 값은 쓰지 않는다")
    void convertsSerializationFailure() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ObjectMapper objectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        AnalysisTargetDTO target = target();
        when(objectMapper.writeValueAsString(target)).thenThrow(
                new JsonProcessingException("serialization failure") { }
        );
        RedisAddressSearchStore store = store(objectMapper, 1_800L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> store.save(target)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        verifyNoInteractions(valueOperations);
    }

    @Test
    @DisplayName("저장된 주소 JSON을 원래 AnalysisTarget으로 복원한다")
    void findsStoredTarget() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisTargetDTO target = target();
        when(valueOperations.get("address-search:address-id"))
                .thenReturn(objectMapper.writeValueAsString(target));
        RedisAddressSearchStore store = store(objectMapper, 1_800L);

        assertThat(store.find("address-id")).isEqualTo(target);
    }

    @Test
    @DisplayName("주소 ID가 없거나 공백이면 Redis를 조회하지 않고 만료 오류를 반환한다")
    void rejectsBlankAddressIdBeforeRedisLookup() {
        RedisAddressSearchStore store = store(new ObjectMapper(), 1_800L);

        for (String addressId : new String[]{null, "", "  "}) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> store.find(addressId)
            );
            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_CONFLICT);
        }
        verifyNoInteractions(redisTemplate, valueOperations);
    }

    @Test
    @DisplayName("Redis에 주소가 없으면 만료 오류를 반환한다")
    void rejectsExpiredAddressId() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("address-search:expired-id")).thenReturn(null);
        RedisAddressSearchStore store = store(new ObjectMapper(), 1_800L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> store.find("expired-id")
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_CONFLICT);
        assertThat(exception).hasMessage("주소 정보가 만료되었습니다. 다시 검색해주세요.");
    }

    @Test
    @DisplayName("손상된 주소 JSON은 즉시 삭제하고 내부 서버 오류를 반환한다")
    void deletesCorruptedJson() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("address-search:broken-id"))
                .thenReturn("not-json");
        RedisAddressSearchStore store = store(new ObjectMapper(), 1_800L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> store.find("broken-id")
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        verify(redisTemplate).delete("address-search:broken-id");
    }

    private RedisAddressSearchStore store(
            ObjectMapper objectMapper,
            long ttlSeconds
    ) {
        RedisAddressSearchStore store = new RedisAddressSearchStore(
                redisTemplate,
                objectMapper
        );
        ReflectionTestUtils.setField(store, "ttlSeconds", ttlSeconds);
        return store;
    }

    private AnalysisTargetDTO target() {
        return new AnalysisTargetDTO(
                "서울 강남구 테헤란로 1",
                "서울특별시 강남구 테헤란로 1",
                "1168010100",
                "11680",
                "10100",
                "1",
                "0",
                "1",
                "0",
                "building-management-no",
                "역삼동",
                "서울특별시 강남구 역삼동 1"
        );
    }
}
