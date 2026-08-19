package com.secondzip.backend.report.dto;

import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import com.secondzip.backend.report.enums.AnalysisNextAction;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisWorkflowState {
    private String requestId;
    private Long accountId;
    private String roadAddress;
    private String detailAddress;
    private Long deposit;
    private AnalysisTarget target;
    private String buildingType;
    private List<BuildingRegisterDocumentType> requiredDocuments;
    private AnalysisRequestStatus status;
    private BuildingRegisterDocumentType pendingDocument;
    private AnalysisNextAction nextAction;
    private CodefTwoWayState twoWayState;
    private List<AnalysisSelectionOption> selectionOptions;
    private List<BuildingRegisterDocumentType> completedDocuments;
    private Map<BuildingRegisterDocumentType, Map<String, Object>> buildingRegisterData;
    private long createdAtEpochMillis;
    private long expiresAtEpochMillis;
    private Long reportId;
    private String buildingUse;
    private String failureMessage;
}
