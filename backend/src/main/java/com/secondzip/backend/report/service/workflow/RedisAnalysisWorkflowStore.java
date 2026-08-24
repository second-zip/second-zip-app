package com.secondzip.backend.report.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AnalysisWorkflowStateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisAnalysisWorkflowStore implements AnalysisWorkflowStore {

    private static final String KEY_PREFIX = "analysis-workflow:";
    private static final String LOCK_PREFIX = "analysis-workflow-lock:";
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] "
                            + "then return redis.call('del', KEYS[1]) else return 0 end",
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 실행 락 TTL.
     *
     * 이 값은 정상 동작 시간이 아니라 장애 복구 시간을 결정.
     * 락은 각 요청이 finally에서 해제하므로, TTL은 서버가 처리 도중
     * 죽었을 때 락이 남아 있는 시간.
     *
     * 트레이드오프:
     *
     *   너무 길면 서버가 죽은 뒤 사용자가 그 시간만큼 "이미 처리 중입니다"만 봄.
     *   워크플로 TTL(기본 900초)보다 길면 사실상 그 요청을 포기해야 함.
     *   너무 짧으면 분석이 끝나기 전에 락이 풀려 같은 요청이 동시에 실행되고,
     *   등기부등본 유료 조회가 중복 과금된다. 이쪽이 더 나쁨.
     *
     * 그래서 가장 느린 경로(외부 API 여러 개 + 등기 조회)를 넉넉히 덮으면서도
     * 워크플로 TTL의 3분의 1 수준인 300초를 기본값으로 둔다.
     */
    @Value("${ANALYSIS_WORKFLOW_LOCK_TTL_SECONDS:300}")
    private long lockTtlSeconds;

    @Override
    public void save(AnalysisWorkflowStateDTO state) {
        long remainingMillis = state.getExpiresAtEpochMillis()
                - System.currentTimeMillis();
        if (remainingMillis <= 0L) {
            delete(state.getRequestId());
            throw workflowNotFound();
        }
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + state.getRequestId(),
                    objectMapper.writeValueAsString(state),
                    Duration.ofMillis(remainingMillis)
            );
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "분석 요청 상태를 저장하지 못했습니다."
            );
        }
    }

    @Override
    public AnalysisWorkflowStateDTO findOwned(String requestId, Long accountId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + requestId);
        if (json == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "분석 요청이 없거나 만료되었습니다."
            );
        }

        try {
            AnalysisWorkflowStateDTO state =
                    objectMapper.readValue(json, AnalysisWorkflowStateDTO.class);
            if (state.getExpiresAtEpochMillis() <= System.currentTimeMillis()) {
                delete(requestId);
                throw workflowNotFound();
            }
            if (!state.getAccountId().equals(accountId)) {
                throw workflowNotFound();
            }
            return state;
        } catch (JsonProcessingException e) {
            delete(requestId);
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "분석 요청 상태를 읽지 못했습니다."
            );
        }
    }

    @Override
    public void delete(String requestId) {
        redisTemplate.delete(KEY_PREFIX + requestId);
    }

    @Override
    public String tryAcquireExecutionLock(String requestId) {
        String lockToken = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                LOCK_PREFIX + requestId,
                lockToken,
                Duration.ofSeconds(Math.max(60L, lockTtlSeconds))
        );
        return Boolean.TRUE.equals(acquired) ? lockToken : null;
    }

    @Override
    public void releaseExecutionLock(String requestId, String lockToken) {
        if (lockToken == null) {
            return;
        }
        redisTemplate.execute(
                RELEASE_LOCK_SCRIPT,
                Collections.singletonList(LOCK_PREFIX + requestId),
                lockToken
        );
    }

    private BusinessException workflowNotFound() {
        return new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "분석 요청이 없거나 만료되었습니다."
        );
    }
}
