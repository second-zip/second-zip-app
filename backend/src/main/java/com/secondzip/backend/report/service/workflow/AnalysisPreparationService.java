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
import com.secondzip.backend.report.service.external.client.AddressSearchCache;
import com.secondzip.backend.report.service.external.client.BuildingHubClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisPreparationService {

    private final AddressClient addressClient;
    private final BuildingHubClient buildingHubClient;
    private final AnalysisWorkflowStore workflowStore;
    private final AddressSearchCache addressSearchCache;

    @Value("${ANALYSIS_WORKFLOW_TTL_SECONDS:900}")
    private long ttlSeconds;

    public AnalysisPreparationResponse prepare(
            Long accountId,
            CreateReportRequest request
    ) {
        AnalysisTarget target = resolveTarget(request);
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
                state.getFailureMessage(),
                state.getPendingDocument(),
                state.getNextAction(),
                state.getSelectionOptions()
        );
    }
    /**
     * 분석 대상 주소를 확정한다.
     *
     * <p>사용자가 검색 화면에서 고른 결과를 토큰으로 받아 그대로 쓴다.
     * 토큰이 없거나 만료됐으면 주소 문자열로 재검색한다.
     *
     * <p>폴백 경로는 검색 결과가 여러 개일 때 첫 번째를 고르므로 사용자가 고른 것과
     * 다를 수 있다. 구버전 프론트 호환용이며, 새 프론트는 항상 토큰을 보낸다.
     */
    private AnalysisTarget resolveTarget(CreateReportRequest request) {
        AnalysisTarget selected = addressSearchCache.find(request.getAddressToken());
        if (selected != null) {
            return selected;
        }
        log.info("주소 토큰이 없어 재검색합니다. roadAddress={}", request.getRoadAddress());
        return addressClient.standardize(request.getRoadAddress());
    }


}
