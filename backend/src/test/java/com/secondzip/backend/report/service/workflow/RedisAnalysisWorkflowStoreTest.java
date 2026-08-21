package com.secondzip.backend.report.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AnalysisWorkflowStateDTO;
import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RedisAnalysisWorkflowStoreTest {

    private static final String REQUEST_ID = "request-1";
    private static final Long ACCOUNT_ID = 11L;
    private static final String WORKFLOW_KEY = "analysis-workflow:" + REQUEST_ID;
    private static final String LOCK_KEY = "analysis-workflow-lock:" + REQUEST_ID;

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private RedisAnalysisWorkflowStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        objectMapper = new ObjectMapper();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        store = new RedisAnalysisWorkflowStore(redisTemplate, objectMapper);
    }

    @Test
    void saveSerializesStateWithRemainingTtl() {
        AnalysisWorkflowStateDTO state = unexpiredState(60_000L);
        org.mockito.ArgumentCaptor<String> jsonCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<Duration> ttlCaptor =
                org.mockito.ArgumentCaptor.forClass(Duration.class);

        store.save(state);

        verify(valueOperations).set(eq(WORKFLOW_KEY), jsonCaptor.capture(), ttlCaptor.capture());
        assertThat(jsonCaptor.getValue()).contains("\"requestId\":\"" + REQUEST_ID + "\"");
        assertThat(ttlCaptor.getValue().toMillis()).isBetween(1L, 60_000L);
    }

    @Test
    void saveDeletesAlreadyExpiredStateAndReturnsNotFound() {
        AnalysisWorkflowStateDTO state = unexpiredState(-1L);

        BusinessException thrown = catchThrowableOfType(
                () -> store.save(state),
                BusinessException.class
        );

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        verify(redisTemplate).delete(WORKFLOW_KEY);
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void saveMapsSerializationFailureToInternalServerError() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        RedisAnalysisWorkflowStore failingStore =
                new RedisAnalysisWorkflowStore(redisTemplate, failingMapper);
        AnalysisWorkflowStateDTO state = unexpiredState(60_000L);
        JsonProcessingException cause = new JsonProcessingException("boom") { };
        when(failingMapper.writeValueAsString(state)).thenThrow(cause);

        BusinessException thrown = catchThrowableOfType(
                () -> failingStore.save(state),
                BusinessException.class
        );

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void findOwnedReturnsValidOwnedState() throws Exception {
        AnalysisWorkflowStateDTO state = unexpiredState(60_000L);
        when(valueOperations.get(WORKFLOW_KEY))
                .thenReturn(objectMapper.writeValueAsString(state));

        AnalysisWorkflowStateDTO found = store.findOwned(REQUEST_ID, ACCOUNT_ID);

        assertThat(found.getRequestId()).isEqualTo(REQUEST_ID);
        assertThat(found.getAccountId()).isEqualTo(ACCOUNT_ID);
        verify(redisTemplate, never()).delete(WORKFLOW_KEY);
    }

    @Test
    void findOwnedHidesMissingWorkflowAsNotFound() {
        when(valueOperations.get(WORKFLOW_KEY)).thenReturn(null);

        BusinessException thrown = catchThrowableOfType(
                () -> store.findOwned(REQUEST_ID, ACCOUNT_ID),
                BusinessException.class
        );

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void findOwnedDeletesExpiredWorkflow() throws Exception {
        AnalysisWorkflowStateDTO state = unexpiredState(-1L);
        when(valueOperations.get(WORKFLOW_KEY))
                .thenReturn(objectMapper.writeValueAsString(state));

        BusinessException thrown = catchThrowableOfType(
                () -> store.findOwned(REQUEST_ID, ACCOUNT_ID),
                BusinessException.class
        );

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        verify(redisTemplate).delete(WORKFLOW_KEY);
    }

    @Test
    void findOwnedHidesAnotherAccountsWorkflowWithoutDeletingIt() throws Exception {
        AnalysisWorkflowStateDTO state = unexpiredState(60_000L);
        when(valueOperations.get(WORKFLOW_KEY))
                .thenReturn(objectMapper.writeValueAsString(state));

        BusinessException thrown = catchThrowableOfType(
                () -> store.findOwned(REQUEST_ID, 999L),
                BusinessException.class
        );

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        verify(redisTemplate, never()).delete(WORKFLOW_KEY);
    }

    @Test
    void findOwnedDeletesCorruptJsonAndReturnsInternalServerError() {
        when(valueOperations.get(WORKFLOW_KEY)).thenReturn("not-json");

        BusinessException thrown = catchThrowableOfType(
                () -> store.findOwned(REQUEST_ID, ACCOUNT_ID),
                BusinessException.class
        );

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        verify(redisTemplate).delete(WORKFLOW_KEY);
    }

    @Test
    void lockUsesUuidTokenAndMinimumSixtySecondTtl() {
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class)))
                .thenReturn(true);
        org.mockito.ArgumentCaptor<String> tokenCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<Duration> ttlCaptor =
                org.mockito.ArgumentCaptor.forClass(Duration.class);

        String token = store.tryAcquireExecutionLock(REQUEST_ID);

        verify(valueOperations).setIfAbsent(
                eq(LOCK_KEY),
                tokenCaptor.capture(),
                ttlCaptor.capture()
        );
        assertThat(token).isEqualTo(tokenCaptor.getValue());
        assertThatCodeIsUuid(token);
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void lockUsesConfiguredTtlAboveMinimum() {
        ReflectionTestUtils.setField(store, "lockTtlSeconds", 125L);
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class)))
                .thenReturn(true);
        org.mockito.ArgumentCaptor<Duration> ttlCaptor =
                org.mockito.ArgumentCaptor.forClass(Duration.class);

        store.tryAcquireExecutionLock(REQUEST_ID);

        verify(valueOperations).setIfAbsent(eq(LOCK_KEY), anyString(), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofSeconds(125));
    }

    @Test
    void lockReturnsNullWhenSetIfAbsentLosesTheRace() {
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class)))
                .thenReturn(false);

        assertThat(store.tryAcquireExecutionLock(REQUEST_ID)).isNull();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void releaseExecutesCompareAndDeleteLuaWithOwnedToken() {
        store.releaseExecutionLock(REQUEST_ID, "owned-token");

        verify(redisTemplate).execute(
                any(DefaultRedisScript.class),
                eq(Collections.singletonList(LOCK_KEY)),
                eq("owned-token")
        );
    }

    @Test
    void releaseWithNullTokenDoesNothing() {
        StringRedisTemplate isolatedRedis = mock(StringRedisTemplate.class);
        RedisAnalysisWorkflowStore isolatedStore =
                new RedisAnalysisWorkflowStore(isolatedRedis, objectMapper);

        isolatedStore.releaseExecutionLock(REQUEST_ID, null);

        verifyNoInteractions(isolatedRedis);
    }

    private AnalysisWorkflowStateDTO unexpiredState(long remainingMillis) {
        AnalysisWorkflowStateDTO state = new AnalysisWorkflowStateDTO();
        state.setRequestId(REQUEST_ID);
        state.setAccountId(ACCOUNT_ID);
        state.setStatus(AnalysisRequestStatus.AUTH_REQUIRED);
        state.setExpiresAtEpochMillis(System.currentTimeMillis() + remainingMillis);
        return state;
    }

    private void assertThatCodeIsUuid(String token) {
        assertThat(UUID.fromString(token).toString()).isEqualTo(token);
    }
}
