package com.secondzip.backend.report.dto;

import com.secondzip.backend.report.dto.external.RegistryData;
import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import com.secondzip.backend.report.enums.AnalysisNextAction;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisWorkflowStateDTO {
    private String requestId;
    private Long accountId;
    private String roadAddress;
    private String detailAddress;
    private Long deposit;
    private AnalysisTargetDTO target;
    private String buildingType;
    private List<BuildingRegisterDocumentType> requiredDocuments;
    private AnalysisRequestStatus status;
    private BuildingRegisterDocumentType pendingDocument;
    private AnalysisNextAction nextAction;
    private CodefTwoWayStateDTO twoWayState;
    private List<AnalysisSelectionOptionDTO> selectionOptions;
    private List<BuildingRegisterDocumentType> completedDocuments;
    private Map<BuildingRegisterDocumentType, Map<String, Object>> buildingRegisterData;
    private long createdAtEpochMillis;
    private long expiresAtEpochMillis;
    private Long reportId;
    private String buildingUse;
    private String failureMessage;
    /** HUB 선택행의 연면적. 일반/표제부 CODEF 면적이 없을 때만 폴백으로 사용. */
    private BigDecimal transactionAreaSqm;

    /**
     * 유료 등기부등본 조회 결과 캐시.
     *
     * executeLocked가 과금이 끝난 등기부 조회 결과를 평가·저장 이전에 이 필드에
     * 담아 둔다. 평가나 저장이 실패해 재시도로 넘어가도 이 값이 남아 있으면
     * registryDataProvider를 다시 호출하지 않고 재사용해 중복 과금을 막는다.
     * 분석이 최종 성공하면 더 이상 필요 없으므로 null로 비운다.
     */
    private RegistryData registryData;

    /** transactionAreaSqm 추가 전 호출부 및 직렬화 테스트 호환용 생성자. */
    public AnalysisWorkflowStateDTO(
            String requestId,
            Long accountId,
            String roadAddress,
            String detailAddress,
            Long deposit,
            AnalysisTargetDTO target,
            String buildingType,
            List<BuildingRegisterDocumentType> requiredDocuments,
            AnalysisRequestStatus status,
            BuildingRegisterDocumentType pendingDocument,
            AnalysisNextAction nextAction,
            CodefTwoWayStateDTO twoWayState,
            List<AnalysisSelectionOptionDTO> selectionOptions,
            List<BuildingRegisterDocumentType> completedDocuments,
            Map<BuildingRegisterDocumentType, Map<String, Object>> buildingRegisterData,
            long createdAtEpochMillis,
            long expiresAtEpochMillis,
            Long reportId,
            String buildingUse,
            String failureMessage
    ) {
        this(
                requestId, accountId, roadAddress, detailAddress, deposit, target,
                buildingType, requiredDocuments, status, pendingDocument, nextAction,
                twoWayState, selectionOptions, completedDocuments, buildingRegisterData,
                createdAtEpochMillis, expiresAtEpochMillis, reportId, buildingUse,
                failureMessage, null, null
        );
    }

    /** registryData(중복 과금 방지 캐시) 추가 전 호출부 호환용 생성자. */
    public AnalysisWorkflowStateDTO(
            String requestId,
            Long accountId,
            String roadAddress,
            String detailAddress,
            Long deposit,
            AnalysisTargetDTO target,
            String buildingType,
            List<BuildingRegisterDocumentType> requiredDocuments,
            AnalysisRequestStatus status,
            BuildingRegisterDocumentType pendingDocument,
            AnalysisNextAction nextAction,
            CodefTwoWayStateDTO twoWayState,
            List<AnalysisSelectionOptionDTO> selectionOptions,
            List<BuildingRegisterDocumentType> completedDocuments,
            Map<BuildingRegisterDocumentType, Map<String, Object>> buildingRegisterData,
            long createdAtEpochMillis,
            long expiresAtEpochMillis,
            Long reportId,
            String buildingUse,
            String failureMessage,
            BigDecimal transactionAreaSqm
    ) {
        this(
                requestId, accountId, roadAddress, detailAddress, deposit, target,
                buildingType, requiredDocuments, status, pendingDocument, nextAction,
                twoWayState, selectionOptions, completedDocuments, buildingRegisterData,
                createdAtEpochMillis, expiresAtEpochMillis, reportId, buildingUse,
                failureMessage, transactionAreaSqm, null
        );
    }
}
