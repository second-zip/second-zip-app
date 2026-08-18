package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.AnalysisWorkflowState;
import com.secondzip.backend.report.dto.RiskEvaluationResult;
import com.secondzip.backend.report.dto.external.BuildingData;
import com.secondzip.backend.report.dto.external.BuildingRegisterAnalysisData;
import com.secondzip.backend.report.dto.external.PriceData;
import com.secondzip.backend.report.dto.external.RegistryData;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import com.secondzip.backend.report.enums.RiskLevel;
import com.secondzip.backend.report.service.ReportPersistenceService;
import com.secondzip.backend.report.service.ReportQueryService;
import com.secondzip.backend.report.service.RiskEvaluationService;
import com.secondzip.backend.report.service.SpecialTermService;
import com.secondzip.backend.report.service.TrustPropertyResolver;
import com.secondzip.backend.report.service.external.client.BuildingRegisterDataParser;
import com.secondzip.backend.report.service.external.client.PriceDataProvider;
import com.secondzip.backend.report.service.external.client.RegistryDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnalysisExecutionServiceBranchTest {

    private static final Long ACCOUNT_ID = 5L;
    private static final String REQUEST_ID = "request-branch";
    private static final String LOCK_TOKEN = "execution-lock";

    private AnalysisWorkflowStore workflowStore;
    private BuildingRegisterDataParser buildingParser;
    private RegistryDataProvider registryProvider;
    private PriceDataProvider priceProvider;
    private RiskEvaluationService riskService;
    private ReportPersistenceService persistenceService;
    private ReportQueryService queryService;
    private SpecialTermService specialTermService;
    private TrustPropertyResolver trustPropertyResolver;
    private AnalysisExecutionService service;

    @BeforeEach
    void setUp() {
        workflowStore = mock(AnalysisWorkflowStore.class);
        buildingParser = mock(BuildingRegisterDataParser.class);
        registryProvider = mock(RegistryDataProvider.class);
        priceProvider = mock(PriceDataProvider.class);
        riskService = mock(RiskEvaluationService.class);
        persistenceService = mock(ReportPersistenceService.class);
        queryService = mock(ReportQueryService.class);
        specialTermService = mock(SpecialTermService.class);
        trustPropertyResolver = mock(TrustPropertyResolver.class);
        service = new AnalysisExecutionService(
                workflowStore,
                buildingParser,
                registryProvider,
                priceProvider,
                riskService,
                persistenceService,
                queryService,
                specialTermService,
                trustPropertyResolver
        );
    }

    @Test
    void executeRejectsLockContentionBeforeReadingWorkflow() {
        when(workflowStore.tryAcquireExecutionLock(REQUEST_ID)).thenReturn(null);

        BusinessException thrown = catchThrowableOfType(
                () -> service.execute(ACCOUNT_ID, REQUEST_ID),
                BusinessException.class
        );

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT);
        verify(workflowStore, never()).findOwned(anyString(), anyLong());
        verify(workflowStore, never()).releaseExecutionLock(anyString(), any());
        verifyNoInteractions(buildingParser, registryProvider, persistenceService, queryService);
    }

    @Test
    void executeAlwaysReleasesLockWhenWorkflowLookupFails() {
        RuntimeException missing = new RuntimeException("redis read failed");
        when(workflowStore.tryAcquireExecutionLock(REQUEST_ID)).thenReturn(LOCK_TOKEN);
        when(workflowStore.findOwned(REQUEST_ID, ACCOUNT_ID)).thenThrow(missing);

        assertThatThrownBy(() -> service.execute(ACCOUNT_ID, REQUEST_ID))
                .isSameAs(missing);

        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
        verifyNoInteractions(buildingParser, registryProvider, persistenceService, queryService);
    }

    @Test
    void executeRejectsPrematureStatusAndReleasesLock() {
        AnalysisWorkflowState state = processingState();
        state.setStatus(AnalysisRequestStatus.AUTH_REQUIRED);
        stubLockAndState(state);

        BusinessException thrown = catchThrowableOfType(
                () -> service.execute(ACCOUNT_ID, REQUEST_ID),
                BusinessException.class
        );

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT);
        verify(buildingParser, never()).parse(anyList(), anyMap(), anyString(), anyString());
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    @Test
    void completedWorkflowReturnsStoredReportWithoutRepeatingPaidWork() {
        AnalysisWorkflowState state = processingState();
        state.setStatus(AnalysisRequestStatus.COMPLETED);
        state.setReportId(40L);
        ReportDetailResponse existing = report(40L);
        stubLockAndState(state);
        when(queryService.getReportDetail(ACCOUNT_ID, 40L)).thenReturn(existing);

        ReportDetailResponse result = service.execute(ACCOUNT_ID, REQUEST_ID);

        assertThat(result).isSameAs(existing);
        verify(registryProvider, never())
                .getRegistryDataForAnalysis(any(), anyString(), anyString());
        verify(persistenceService, never()).save(
                anyLong(), anyString(), anyString(), anyString(), anyLong(),
                any(), anyString(), anyBoolean()
        );
        verify(workflowStore, never()).save(any());
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    @Test
    void databaseRequestIdWinsWhenRedisStateWasNotMarkedCompleted() {
        AnalysisWorkflowState state = processingState();
        state.setFailureMessage("old failure");
        ReportDetailResponse existing = report(41L);
        stubLockAndState(state);
        when(queryService.findReportIdByRequestId(ACCOUNT_ID, REQUEST_ID)).thenReturn(41L);
        when(queryService.getReportDetail(ACCOUNT_ID, 41L)).thenReturn(existing);

        ReportDetailResponse result = service.execute(ACCOUNT_ID, REQUEST_ID);

        assertThat(result).isSameAs(existing);
        assertThat(state.getStatus()).isEqualTo(AnalysisRequestStatus.COMPLETED);
        assertThat(state.getReportId()).isEqualTo(41L);
        assertThat(state.getFailureMessage()).isNull();
        assertThat(state.getBuildingRegisterData()).isEmpty();
        verify(workflowStore).save(state);
        verify(registryProvider, never())
                .getRegistryDataForAnalysis(any(), anyString(), anyString());
        verify(persistenceService, never()).save(
                anyLong(), anyString(), anyString(), anyString(), anyLong(),
                any(), anyString(), anyBoolean()
        );
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    @Test
    void retryRejectsStateThatHasNotFailed() {
        AnalysisWorkflowState state = processingState();
        stubLockAndState(state);

        BusinessException thrown = catchThrowableOfType(
                () -> service.retry(ACCOUNT_ID, REQUEST_ID),
                BusinessException.class
        );

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT);
        verify(workflowStore, never()).save(any());
        verifyNoInteractions(queryService);
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    @Test
    void retryRejectsMissingRequiredDocumentMetadata() {
        AnalysisWorkflowState state = failedState();
        state.setRequiredDocuments(null);
        stubLockAndState(state);

        assertRetryNeedsCompletedAuthentication();
    }

    @Test
    void retryRejectsMissingCompletedDocumentMetadata() {
        AnalysisWorkflowState state = failedState();
        state.setCompletedDocuments(null);
        stubLockAndState(state);

        assertRetryNeedsCompletedAuthentication();
    }

    @Test
    void retryRejectsPartialDocumentCompletion() {
        AnalysisWorkflowState state = failedState();
        state.setRequiredDocuments(List.of(
                BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE
        ));
        state.setCompletedDocuments(new ArrayList<>(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_TITLE)
        ));
        stubLockAndState(state);

        assertRetryNeedsCompletedAuthentication();
    }

    @Test
    void retryReleasesLockWhenPreparingRetryStateCannotBeSaved() {
        AnalysisWorkflowState state = failedState();
        RuntimeException redisFailure = new RuntimeException("redis write failed");
        stubLockAndState(state);
        doThrow(redisFailure).when(workflowStore).save(state);

        assertThatThrownBy(() -> service.retry(ACCOUNT_ID, REQUEST_ID))
                .isSameAs(redisFailure);

        assertThat(state.getStatus()).isEqualTo(AnalysisRequestStatus.PROCESSING);
        assertThat(state.getFailureMessage()).isNull();
        verifyNoInteractions(queryService);
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    @Test
    void retryTransitionsThroughProcessingAndReusesExistingDatabaseReport() {
        AnalysisWorkflowState state = failedState();
        List<AnalysisRequestStatus> savedStatuses = new ArrayList<>();
        ReportDetailResponse existing = report(42L);
        stubLockAndState(state);
        doAnswer(invocation -> {
            savedStatuses.add(state.getStatus());
            return null;
        }).when(workflowStore).save(state);
        when(queryService.findReportIdByRequestId(ACCOUNT_ID, REQUEST_ID)).thenReturn(42L);
        when(queryService.getReportDetail(ACCOUNT_ID, 42L)).thenReturn(existing);

        ReportDetailResponse result = service.retry(ACCOUNT_ID, REQUEST_ID);

        assertThat(result).isSameAs(existing);
        assertThat(savedStatuses).containsExactly(
                AnalysisRequestStatus.PROCESSING,
                AnalysisRequestStatus.COMPLETED
        );
        assertThat(state.getFailureMessage()).isNull();
        assertThat(state.getBuildingRegisterData()).isEmpty();
        verify(workflowStore, times(2)).findOwned(REQUEST_ID, ACCOUNT_ID);
        verify(registryProvider, never())
                .getRegistryDataForAnalysis(any(), anyString(), anyString());
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    @Test
    void failureStateSaveCannotMaskOriginalBusinessFailure() {
        AnalysisWorkflowState state = processingState();
        BusinessException original = new BusinessException(
                ErrorCode.EXTERNAL_API_ERROR,
                "건축물대장 원본이 손상되었습니다."
        );
        stubLockAndState(state);
        when(buildingParser.parse(anyList(), anyMap(), anyString(), anyString()))
                .thenThrow(original);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(workflowStore).save(state);

        assertThatThrownBy(() -> service.execute(ACCOUNT_ID, REQUEST_ID))
                .isSameAs(original);

        assertThat(state.getStatus()).isEqualTo(AnalysisRequestStatus.FAILED);
        assertThat(state.getFailureMessage()).isEqualTo(original.getMessage());
        verify(workflowStore).save(state);
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    @Test
    void unexpectedFailureUsesSafeGenericFailureMessage() {
        AnalysisWorkflowState state = processingState();
        IllegalStateException original = new IllegalStateException("secret upstream detail");
        stubLockAndState(state);
        when(buildingParser.parse(anyList(), anyMap(), anyString(), anyString()))
                .thenThrow(original);

        assertThatThrownBy(() -> service.execute(ACCOUNT_ID, REQUEST_ID))
                .isSameAs(original);

        assertThat(state.getStatus()).isEqualTo(AnalysisRequestStatus.FAILED);
        assertThat(state.getFailureMessage())
                .isEqualTo("외부 데이터 조회 또는 리포트 저장 중 오류가 발생했습니다.");
        verify(workflowStore).save(state);
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    @Test
    void completionStateSaveFailureDoesNotDiscardPersistedReport() {
        AnalysisWorkflowState state = processingState();
        ReportDetailResponse persisted = report(43L);
        ReportDetailResponse enriched = report(43L);
        stubLockAndState(state);
        stubSuccessfulPaidAnalysis(persisted);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(workflowStore).save(state);
        when(queryService.getReportDetail(ACCOUNT_ID, 43L)).thenReturn(enriched);

        ReportDetailResponse result = service.execute(ACCOUNT_ID, REQUEST_ID);

        assertThat(result).isSameAs(enriched);
        assertThat(state.getStatus()).isEqualTo(AnalysisRequestStatus.COMPLETED);
        assertThat(state.getReportId()).isEqualTo(43L);
        assertThat(state.getBuildingRegisterData()).isEmpty();
        verify(specialTermService).generateAndSave(ACCOUNT_ID, 43L);
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    @Test
    void specialTermFailureIsIsolatedFromSuccessfulAnalysis() {
        AnalysisWorkflowState state = processingState();
        ReportDetailResponse persisted = report(44L);
        stubLockAndState(state);
        stubSuccessfulPaidAnalysis(persisted);
        when(specialTermService.generateAndSave(ACCOUNT_ID, 44L))
                .thenThrow(new IllegalStateException("gpt unavailable"));

        ReportDetailResponse result = service.execute(ACCOUNT_ID, REQUEST_ID);

        assertThat(result).isSameAs(persisted);
        assertThat(state.getStatus()).isEqualTo(AnalysisRequestStatus.COMPLETED);
        assertThat(state.getReportId()).isEqualTo(44L);
        verify(queryService, never()).getReportDetail(ACCOUNT_ID, 44L);
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    private void assertRetryNeedsCompletedAuthentication() {
        BusinessException thrown = catchThrowableOfType(
                () -> service.retry(ACCOUNT_ID, REQUEST_ID),
                BusinessException.class
        );

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT);
        assertThat(thrown.getMessage()).contains("인증부터 다시");
        verify(workflowStore, never()).save(any());
        verifyNoInteractions(queryService);
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    private void stubLockAndState(AnalysisWorkflowState state) {
        when(workflowStore.tryAcquireExecutionLock(REQUEST_ID)).thenReturn(LOCK_TOKEN);
        when(workflowStore.findOwned(REQUEST_ID, ACCOUNT_ID)).thenReturn(state);
        when(queryService.findReportIdByRequestId(ACCOUNT_ID, REQUEST_ID))
                .thenReturn(null);
    }

    private void stubSuccessfulPaidAnalysis(ReportDetailResponse persisted) {
        BuildingData building = new BuildingData();
        building.setIllegalBuildingVerified(true);
        BuildingRegisterAnalysisData parsed =
                new BuildingRegisterAnalysisData(building, null, new LinkedHashMap<>());
        PriceData price = new PriceData();
        price.setRecentSalePrice(800_000_000L);
        RegistryData registry = new RegistryData();
        registry.setMortgageAmount(0L);
        registry.setHasSeizure(false);
        registry.setHasTrustRegistration(false);
        RiskEvaluationResult evaluation =
                new RiskEvaluationResult(RiskLevel.SAFE, List.of(), List.of());

        when(buildingParser.parse(anyList(), anyMap(), anyString(), anyString()))
                .thenReturn(parsed);
        when(priceProvider.getPriceData(any(), anyString())).thenReturn(price);
        when(registryProvider.getRegistryDataForAnalysis(any(), anyString(), anyString()))
                .thenReturn(registry);
        when(riskService.evaluate(
                eq(registry), eq(building), eq(price), anyLong(), anyString()
        )).thenReturn(evaluation);
        when(trustPropertyResolver.resolve(registry)).thenReturn(false);
        when(persistenceService.save(
                anyLong(), anyString(), anyString(), anyString(), anyLong(),
                eq(evaluation), anyString(), anyBoolean()
        )).thenReturn(persisted);
    }

    private AnalysisWorkflowState failedState() {
        AnalysisWorkflowState state = processingState();
        state.setStatus(AnalysisRequestStatus.FAILED);
        state.setFailureMessage("temporary external failure");
        state.setCompletedDocuments(new ArrayList<>(state.getRequiredDocuments()));
        return state;
    }

    private AnalysisWorkflowState processingState() {
        AnalysisTarget target = new AnalysisTarget(
                "서울 강남구 테헤란로 152",
                "서울 강남구 테헤란로 152",
                "1168010100",
                "11680",
                "10100",
                "737",
                "",
                "152",
                "",
                ""
        );
        AnalysisWorkflowState state = new AnalysisWorkflowState();
        state.setRequestId(REQUEST_ID);
        state.setAccountId(ACCOUNT_ID);
        state.setRoadAddress(target.roadAddress());
        state.setDetailAddress("101동 1203호");
        state.setDeposit(500_000_000L);
        state.setTarget(target);
        state.setBuildingType("APARTMENT");
        state.setBuildingUse("공동주택");
        state.setRequiredDocuments(new ArrayList<>(
                List.of(BuildingRegisterDocumentType.GENERAL)
        ));
        state.setCompletedDocuments(new ArrayList<>(
                List.of(BuildingRegisterDocumentType.GENERAL)
        ));
        state.setBuildingRegisterData(new LinkedHashMap<>());
        state.setStatus(AnalysisRequestStatus.PROCESSING);
        return state;
    }

    private ReportDetailResponse report(long id) {
        return new ReportDetailResponse(
                id,
                "서울 강남구 테헤란로 152",
                "101동 1203호",
                500_000_000L,
                RiskLevel.SAFE,
                false,
                "APARTMENT",
                false,
                List.of(),
                List.of(),
                List.of()
        );
    }
}
