package com.secondzip.backend.report.dto;

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
                failureMessage, null
        );
    }
}
