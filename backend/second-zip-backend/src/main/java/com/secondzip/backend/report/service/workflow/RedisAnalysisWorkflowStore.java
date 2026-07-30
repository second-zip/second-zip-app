package com.secondzip.backend.report.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AnalysisWorkflowState;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisAnalysisWorkflowStore implements AnalysisWorkflowStore {

    private static final String KEY_PREFIX = "analysis-workflow:";
    private static final String LOCK_PREFIX = "analysis-workflow-lock:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ANALYSIS_WORKFLOW_TTL_SECONDS:900}")
    private long ttlSeconds;

    @Override
    public void save(AnalysisWorkflowState state) {
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + state.getRequestId(),
                    objectMapper.writeValueAsString(state),
                    Duration.ofSeconds(Math.max(60L, ttlSeconds))
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
            if (!state.getAccountId().equals(accountId)) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
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
    public boolean tryAcquireExecutionLock(String requestId) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                LOCK_PREFIX + requestId,
                "1",
                Duration.ofMinutes(5)
        );
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void releaseExecutionLock(String requestId) {
        redisTemplate.delete(LOCK_PREFIX + requestId);
    }
}
