package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.AnalysisWorkflowState;
import com.secondzip.backend.report.dto.external.BuildingData;
import com.secondzip.backend.report.dto.request.CreateReportRequest;
import com.secondzip.backend.report.dto.response.AnalysisPreparationResponse;
import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import com.secondzip.backend.report.service.AddressSearchStore;
import com.secondzip.backend.report.service.external.client.BuildingHubClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalysisPreparationService {

    private final AddressSearchStore addressSearchStore;
    private final BuildingHubClient buildingHubClient;
    private final AnalysisWorkflowStore workflowStore;

    @Value("${ANALYSIS_WORKFLOW_TTL_SECONDS:900}")
    private long ttlSeconds;

    public AnalysisPreparationResponse prepare(
            Long accountId,
            CreateReportRequest request
    ) {
        // 주소 검색(AN_19) 때 확보해 둔 식별값을 꺼내 쓴다.
        // 여기서 주소를 다시 검색하지 않으므로, 사용자가 고른 주소와 분석 대상이 어긋날 수 없다.
        AnalysisTarget target = addressSearchStore.find(request.getAddressId());

        BuildingData building = buildingHubClient.getBuildingData(target);
        if (building == null || building.getBuildingType() == null) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "건축물 유형을 확인하지 못했습니다."
            );
        }

        List<BuildingRegisterDocumentType> requiredDocuments =
                BuildingRegisterDocumentSelector.select(building.getBuildingType());
        if (requiredDocuments.contains(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE)
                && (request.getDetailAddress() == null
                || request.getDetailAddress().isBlank())) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "집합건물은 동·호 상세주소가 필요합니다."
            );
        }
        long now = System.currentTimeMillis();
        long expiresAt = now + Math.max(60L, ttlSeconds) * 1000L;
        String requestId = UUID.randomUUID().toString();

        AnalysisWorkflowState state = new AnalysisWorkflowState(
                requestId,
                accountId,
                target.roadAddress(),
                request.getDetailAddress(),
                request.getDeposit(),
                target,
                building.getBuildingType(),
                requiredDocuments,
                AnalysisRequestStatus.AUTH_REQUIRED,
                null,
                null,
                null,
                List.of(),
                new java.util.ArrayList<>(),
                new java.util.LinkedHashMap<>(),
                now,
                expiresAt,
                null,
                building.getBuildingUse(),
                null
        );
        workflowStore.save(state);

        return toResponse(state);
    }

    public AnalysisPreparationResponse getStatus(Long accountId, String requestId) {
        return toResponse(workflowStore.findOwned(requestId, accountId));
    }

    private AnalysisPreparationResponse toResponse(AnalysisWorkflowState state) {
        return new AnalysisPreparationResponse(
                state.getRequestId(),
                state.getStatus(),
                state.getTarget().roadAddress(),
                state.getBuildingType(),
                state.getRequiredDocuments(),
                state.getStatus() == AnalysisRequestStatus.AUTH_REQUIRED
                        || state.getStatus() == AnalysisRequestStatus.AUTH_PENDING,
                state.getExpiresAtEpochMillis(),
                state.getReportId(),
                state.getFailureMessage(),
                state.getPendingDocument(),
                state.getNextAction(),
                state.getSelectionOptions()
        );
    }
}
