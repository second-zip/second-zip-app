package com.secondzip.backend.report.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AnalysisWorkflowState;
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

    @Value("${ANALYSIS_WORKFLOW_LOCK_TTL_SECONDS:600}")
    private long lockTtlSeconds;

    @Override
    public void save(AnalysisWorkflowState state) {
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
    public AnalysisWorkflowState findOwned(String requestId, Long accountId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + requestId);
        if (json == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "분석 요청이 없거나 만료되었습니다."
            );
        }

        try {
            AnalysisWorkflowState state =
                    objectMapper.readValue(json, AnalysisWorkflowState.class);
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
