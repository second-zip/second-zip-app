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
import com.secondzip.backend.report.service.external.client.AddressClient;
import com.secondzip.backend.report.service.external.client.BuildingHubClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalysisPreparationService {

    private final AddressClient addressClient;
    private final BuildingHubClient buildingHubClient;
    private final AnalysisWorkflowStore workflowStore;

    @Value("${ANALYSIS_WORKFLOW_TTL_SECONDS:900}")
    private long ttlSeconds;

    public AnalysisPreparationResponse prepare(
            Long accountId,
            CreateReportRequest request
    ) {
        AnalysisTarget target = addressClient.standardize(request.getRoadAddress());
        if (target == null) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "입력한 주소를 표준화하지 못했습니다."
            );
        }

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
                request.getRoadAddress(),
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
                state.getFailureMessage()
        );
    }
}
