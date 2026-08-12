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

        verifySelectedAddress(request, target);

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

    /**
     * 사용자가 검색 화면에서 고른 주소와 백엔드가 표준화한 주소가 같은 곳인지 확인한다.
     *
     * <p>프론트는 주소 문자열만 보내고 백엔드는 그 문자열로 다시 검색해 첫 번째 결과를 쓴다.
     * 이때 카카오가 다른 지역의 동명 도로를 먼저 반환하면 사용자가 고른 것과 다른 건물이
     * 분석 대상이 된다. 그 상태로 건축물대장과 등기부(건당 700원)를 조회하게 되므로
     * <b>유료 호출 이전인 이 시점에</b> 막는다.
     *
     * <p><b>법정동 단위까지만 검증한다.</b> 법정동코드 10자리는 시군구 5 + 읍면동 5라
     * 같은 동 안에서 번지가 어긋나는 경우는 잡지 못한다. 주소 검색 위젯이 본번·부번을
     * 별도 필드로 주지 않아 지번 주소 문자열을 파싱해야 하는데, 그 파싱 오류가 더
     * 위험하다고 보고 코드 비교만 한다.
     *
     * <p>값을 보내지 않으면 검증을 건너뛴다. 구버전 프론트 호환을 위해서다.
     */
    private void verifySelectedAddress(
            CreateReportRequest request,
            AnalysisTarget target
    ) {
        String selected = request.getLegalDongCode();
        if (selected == null || selected.isBlank()) {
            return;
        }
        if (!selected.equals(target.legalDongCode())) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "선택한 주소와 조회된 주소가 일치하지 않습니다. 다시 검색해주세요."
            );
        }
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
