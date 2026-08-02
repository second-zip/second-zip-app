package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AnalysisWorkflowState;
import com.secondzip.backend.report.dto.request.StartAnalysisAuthRequest;
import com.secondzip.backend.report.dto.request.ContinueAnalysisAuthRequest;
import com.secondzip.backend.report.dto.response.AnalysisAuthResponse;
import com.secondzip.backend.report.enums.AnalysisNextAction;
import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import com.secondzip.backend.report.service.external.client.BuildingRegisterGateway;
import com.secondzip.backend.report.service.external.client.BuildingRegisterGatewayResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisAuthenticationService {

    private final AnalysisWorkflowStore workflowStore;
    private final BuildingRegisterGateway buildingRegisterGateway;

    public AnalysisAuthResponse start(
            Long accountId,
            String requestId,
            StartAnalysisAuthRequest authRequest
    ) {
        AnalysisWorkflowState state = workflowStore.findOwned(requestId, accountId);
        if (state.getStatus() != AnalysisRequestStatus.AUTH_REQUIRED) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "현재 상태에서는 인증을 시작할 수 없습니다."
            );
        }

        BuildingRegisterDocumentType documentType = nextDocument(state);
        BuildingRegisterGatewayResult result =
                buildingRegisterGateway.start(state, documentType, authRequest);

        applyResult(state, documentType, result);
        workflowStore.save(state);

        return toResponse(state, result.getCaptchaImage());
    }

    public AnalysisAuthResponse continueAuthentication(
            Long accountId,
            String requestId,
            ContinueAnalysisAuthRequest request
    ) {
        AnalysisWorkflowState state = workflowStore.findOwned(requestId, accountId);
        if (state.getStatus() != AnalysisRequestStatus.AUTH_PENDING
                && state.getStatus() != AnalysisRequestStatus.SELECTION_REQUIRED) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "현재 상태에서는 추가인증을 진행할 수 없습니다."
            );
        }

        BuildingRegisterDocumentType documentType = state.getPendingDocument();
        BuildingRegisterGatewayResult result =
                buildingRegisterGateway.continueRequest(state, request);
        applyResult(state, documentType, result);
        workflowStore.save(state);
        return toResponse(state, result.getCaptchaImage());
    }

    private void applyResult(
            AnalysisWorkflowState state,
            BuildingRegisterDocumentType documentType,
            BuildingRegisterGatewayResult result
    ) {
        if (result.isCompleted()) {
            state.getBuildingRegisterData().put(documentType, result.getData());
            if (!state.getCompletedDocuments().contains(documentType)) {
                state.getCompletedDocuments().add(documentType);
            }
            state.setPendingDocument(null);
            state.setNextAction(AnalysisNextAction.NONE);
            state.setTwoWayState(null);
            state.setSelectionOptions(java.util.List.of());
            state.setStatus(
                    hasRemainingDocument(state)
                            ? AnalysisRequestStatus.AUTH_REQUIRED
                            : AnalysisRequestStatus.PROCESSING
            );
            return;
        }

        state.setPendingDocument(documentType);
        state.setNextAction(result.getNextAction());
        state.setTwoWayState(result.getTwoWayState());
        state.setSelectionOptions(result.getSelectionOptions());
        state.setStatus(
                result.getNextAction() == AnalysisNextAction.SIMPLE_AUTH
                        ? AnalysisRequestStatus.AUTH_PENDING
                        : AnalysisRequestStatus.SELECTION_REQUIRED
        );
    }

    private BuildingRegisterDocumentType nextDocument(AnalysisWorkflowState state) {
        return state.getRequiredDocuments().stream()
                .filter(type -> !state.getCompletedDocuments().contains(type))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_CONFLICT,
                        "추가로 발급할 건축물대장 문서가 없습니다."
                ));
    }

    private boolean hasRemainingDocument(AnalysisWorkflowState state) {
        return state.getRequiredDocuments().stream()
                .anyMatch(type -> !state.getCompletedDocuments().contains(type));
    }

    private AnalysisAuthResponse toResponse(
            AnalysisWorkflowState state,
            String captchaImage
    ) {
        return new AnalysisAuthResponse(
                state.getRequestId(),
                state.getStatus(),
                state.getPendingDocument(),
                state.getNextAction(),
                state.getSelectionOptions(),
                captchaImage,
                state.getExpiresAtEpochMillis()
        );
    }
}
