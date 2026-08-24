package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AnalysisSelectionOptionDTO;
import com.secondzip.backend.report.dto.AnalysisWorkflowStateDTO;
import com.secondzip.backend.report.dto.CodefTwoWayStateDTO;
import com.secondzip.backend.report.dto.request.ContinueAnalysisAuthRequest;
import com.secondzip.backend.report.dto.request.StartAnalysisAuthRequest;
import com.secondzip.backend.report.dto.response.AnalysisAuthResponse;
import com.secondzip.backend.report.enums.AnalysisNextAction;
import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import com.secondzip.backend.report.service.external.client.BuildingRegisterGateway;
import com.secondzip.backend.report.service.external.client.BuildingRegisterGatewayResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnalysisAuthenticationServiceTest {

    private static final Long ACCOUNT_ID = 7L;
    private static final String REQUEST_ID = "workflow-1";
    private static final String LOCK_TOKEN = "lock-token";

    private AnalysisWorkflowStore workflowStore;
    private BuildingRegisterGateway gateway;
    private AnalysisAuthenticationService service;

    @BeforeEach
    void setUp() {
        workflowStore = mock(AnalysisWorkflowStore.class);
        gateway = mock(BuildingRegisterGateway.class);
        service = new AnalysisAuthenticationService(workflowStore, gateway);
    }

    @Test
    void rejectsStartWhenAnotherExecutionOwnsTheLock() {
        when(workflowStore.tryAcquireExecutionLock(REQUEST_ID)).thenReturn(null);

        BusinessException thrown = catchThrowableOfType(
                () -> service.start(ACCOUNT_ID, REQUEST_ID, new StartAnalysisAuthRequest()),
                BusinessException.class
        );

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT);
        verify(workflowStore, never()).findOwned(any(), any());
        verify(workflowStore, never()).releaseExecutionLock(any(), any());
        verifyNoInteractions(gateway);
    }

    @Test
    void releasesLockWhenGatewayStartFails() {
        AnalysisWorkflowStateDTO state = state(
                AnalysisRequestStatus.AUTH_REQUIRED,
                List.of(BuildingRegisterDocumentType.GENERAL),
                List.of()
        );
        IllegalStateException failure = new IllegalStateException("gateway down");
        stubOwnedState(state);
        when(gateway.start(eq(state), eq(BuildingRegisterDocumentType.GENERAL), any()))
                .thenThrow(failure);

        assertThatThrownBy(
                () -> service.start(ACCOUNT_ID, REQUEST_ID, new StartAnalysisAuthRequest())
        ).isSameAs(failure);

        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
        verify(workflowStore, never()).save(any());
    }

    @Test
    void completedStartMovesToNextDocumentAndClearsTwoWayState() {
        AnalysisWorkflowStateDTO state = state(
                AnalysisRequestStatus.AUTH_REQUIRED,
                List.of(
                        BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                        BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE
                ),
                List.of()
        );
        state.setPendingDocument(BuildingRegisterDocumentType.COLLECTIVE_TITLE);
        state.setNextAction(AnalysisNextAction.CAPTCHA);
        state.setTwoWayState(new CodefTwoWayStateDTO(1, 2, "jti", "timestamp"));
        state.setSelectionOptions(List.of(new AnalysisSelectionOptionDTO("1", "one")));
        Map<String, Object> data = Map.of("resViolationStatus", "정상");
        stubOwnedState(state);
        when(gateway.start(eq(state), eq(BuildingRegisterDocumentType.COLLECTIVE_TITLE), any()))
                .thenReturn(completed(data));

        AnalysisAuthResponse response = service.start(
                ACCOUNT_ID,
                REQUEST_ID,
                new StartAnalysisAuthRequest()
        );

        assertThat(state.getStatus()).isEqualTo(AnalysisRequestStatus.AUTH_REQUIRED);
        assertThat(state.getCompletedDocuments())
                .containsExactly(BuildingRegisterDocumentType.COLLECTIVE_TITLE);
        assertThat(state.getBuildingRegisterData())
                .containsEntry(BuildingRegisterDocumentType.COLLECTIVE_TITLE, data);
        assertThat(state.getPendingDocument()).isNull();
        assertThat(state.getNextAction()).isEqualTo(AnalysisNextAction.NONE);
        assertThat(state.getTwoWayState()).isNull();
        assertThat(state.getSelectionOptions()).isEmpty();
        assertThat(response.getStatus()).isEqualTo(AnalysisRequestStatus.AUTH_REQUIRED);
        verify(workflowStore).save(state);
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    @Test
    void incompleteSimpleAuthenticationPersistsContinuationContext() {
        AnalysisWorkflowStateDTO state = state(
                AnalysisRequestStatus.AUTH_REQUIRED,
                List.of(BuildingRegisterDocumentType.GENERAL),
                List.of()
        );
        CodefTwoWayStateDTO twoWay = new CodefTwoWayStateDTO(3, 4, "jti", "timestamp");
        List<AnalysisSelectionOptionDTO> options =
                List.of(new AnalysisSelectionOptionDTO("PASS", "인증 완료"));
        BuildingRegisterGatewayResult pending = new BuildingRegisterGatewayResult(
                false,
                AnalysisNextAction.SIMPLE_AUTH,
                twoWay,
                options,
                "captcha",
                null
        );
        stubOwnedState(state);
        when(gateway.start(eq(state), eq(BuildingRegisterDocumentType.GENERAL), any()))
                .thenReturn(pending);

        AnalysisAuthResponse response = service.start(
                ACCOUNT_ID,
                REQUEST_ID,
                new StartAnalysisAuthRequest()
        );

        assertThat(state.getStatus()).isEqualTo(AnalysisRequestStatus.AUTH_PENDING);
        assertThat(state.getPendingDocument())
                .isEqualTo(BuildingRegisterDocumentType.GENERAL);
        assertThat(state.getNextAction()).isEqualTo(AnalysisNextAction.SIMPLE_AUTH);
        assertThat(state.getTwoWayState()).isSameAs(twoWay);
        assertThat(state.getSelectionOptions()).isSameAs(options);
        assertThat(response.getCaptchaImage()).isEqualTo("captcha");
        assertThat(response.getExpiresAtEpochMillis())
                .isEqualTo(state.getExpiresAtEpochMillis());
    }

    @Test
    void incompleteContinuationMovesToSelectionRequired() {
        AnalysisWorkflowStateDTO state = state(
                AnalysisRequestStatus.AUTH_PENDING,
                List.of(BuildingRegisterDocumentType.GENERAL),
                List.of()
        );
        state.setPendingDocument(BuildingRegisterDocumentType.GENERAL);
        BuildingRegisterGatewayResult pending = new BuildingRegisterGatewayResult(
                false,
                AnalysisNextAction.HO_SELECTION,
                new CodefTwoWayStateDTO(),
                List.of(new AnalysisSelectionOptionDTO("1203", "1203호")),
                null,
                null
        );
        stubOwnedState(state);
        when(gateway.continueRequest(eq(state), any())).thenReturn(pending);

        AnalysisAuthResponse response = service.continueAuthentication(
                ACCOUNT_ID,
                REQUEST_ID,
                new ContinueAnalysisAuthRequest()
        );

        assertThat(state.getStatus()).isEqualTo(AnalysisRequestStatus.SELECTION_REQUIRED);
        assertThat(response.getNextAction()).isEqualTo(AnalysisNextAction.HO_SELECTION);
        verify(workflowStore).save(state);
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    @Test
    void completedContinuationDoesNotDuplicateDocumentAndStartsProcessing() {
        AnalysisWorkflowStateDTO state = state(
                AnalysisRequestStatus.SELECTION_REQUIRED,
                List.of(BuildingRegisterDocumentType.GENERAL),
                List.of(BuildingRegisterDocumentType.GENERAL)
        );
        state.setPendingDocument(BuildingRegisterDocumentType.GENERAL);
        stubOwnedState(state);
        when(gateway.continueRequest(eq(state), any()))
                .thenReturn(completed(Map.of("key", "new-value")));

        service.continueAuthentication(
                ACCOUNT_ID,
                REQUEST_ID,
                new ContinueAnalysisAuthRequest()
        );

        assertThat(state.getCompletedDocuments())
                .containsExactly(BuildingRegisterDocumentType.GENERAL);
        assertThat(state.getStatus()).isEqualTo(AnalysisRequestStatus.PROCESSING);
        assertThat(state.getBuildingRegisterData())
                .containsEntry(
                        BuildingRegisterDocumentType.GENERAL,
                        Map.of("key", "new-value")
                );
    }

    @Test
    void invalidStartStatusSkipsGatewayAndStillReleasesLock() {
        AnalysisWorkflowStateDTO state = state(
                AnalysisRequestStatus.PROCESSING,
                List.of(BuildingRegisterDocumentType.GENERAL),
                List.of(BuildingRegisterDocumentType.GENERAL)
        );
        stubOwnedState(state);

        BusinessException thrown = catchThrowableOfType(
                () -> service.start(ACCOUNT_ID, REQUEST_ID, new StartAnalysisAuthRequest()),
                BusinessException.class
        );

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT);
        verifyNoInteractions(gateway);
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    @Test
    void startFailsWhenAuthRequiredStateHasNoRemainingDocument() {
        AnalysisWorkflowStateDTO state = state(
                AnalysisRequestStatus.AUTH_REQUIRED,
                List.of(BuildingRegisterDocumentType.GENERAL),
                List.of(BuildingRegisterDocumentType.GENERAL)
        );
        stubOwnedState(state);

        BusinessException thrown = catchThrowableOfType(
                () -> service.start(ACCOUNT_ID, REQUEST_ID, new StartAnalysisAuthRequest()),
                BusinessException.class
        );

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT);
        verifyNoInteractions(gateway);
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    @Test
    void invalidContinuationStatusSkipsGatewayAndStillReleasesLock() {
        AnalysisWorkflowStateDTO state = state(
                AnalysisRequestStatus.AUTH_REQUIRED,
                List.of(BuildingRegisterDocumentType.GENERAL),
                List.of()
        );
        stubOwnedState(state);

        BusinessException thrown = catchThrowableOfType(
                () -> service.continueAuthentication(
                        ACCOUNT_ID,
                        REQUEST_ID,
                        new ContinueAnalysisAuthRequest()
                ),
                BusinessException.class
        );

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT);
        verifyNoInteractions(gateway);
        verify(workflowStore).releaseExecutionLock(REQUEST_ID, LOCK_TOKEN);
    }

    private void stubOwnedState(AnalysisWorkflowStateDTO state) {
        when(workflowStore.tryAcquireExecutionLock(REQUEST_ID)).thenReturn(LOCK_TOKEN);
        when(workflowStore.findOwned(REQUEST_ID, ACCOUNT_ID)).thenReturn(state);
    }

    private AnalysisWorkflowStateDTO state(
            AnalysisRequestStatus status,
            List<BuildingRegisterDocumentType> required,
            List<BuildingRegisterDocumentType> completed
    ) {
        AnalysisWorkflowStateDTO state = new AnalysisWorkflowStateDTO();
        state.setRequestId(REQUEST_ID);
        state.setAccountId(ACCOUNT_ID);
        state.setStatus(status);
        state.setRequiredDocuments(new ArrayList<>(required));
        state.setCompletedDocuments(new ArrayList<>(completed));
        state.setBuildingRegisterData(new LinkedHashMap<>());
        state.setSelectionOptions(new ArrayList<>());
        state.setNextAction(AnalysisNextAction.NONE);
        state.setExpiresAtEpochMillis(System.currentTimeMillis() + 60_000L);
        return state;
    }

    private BuildingRegisterGatewayResult completed(Map<String, Object> data) {
        return new BuildingRegisterGatewayResult(
                true,
                AnalysisNextAction.NONE,
                null,
                List.of(),
                null,
                data
        );
    }
}
