package com.secondzip.backend.report.dto.response;

import com.secondzip.backend.report.dto.AnalysisSelectionOption;
import com.secondzip.backend.report.enums.AnalysisNextAction;
import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AnalysisPreparationResponse {
    private final String requestId;
    private final AnalysisRequestStatus status;
    private final String standardizedRoadAddress;
    private final String buildingType;
    private final List<BuildingRegisterDocumentType> requiredDocuments;
    private final boolean authenticationRequired;
    private final long expiresAtEpochMillis;
    private final Long reportId;
    private final String failureMessage;
    private final BuildingRegisterDocumentType pendingDocument;
    private final AnalysisNextAction nextAction;
    private final List<AnalysisSelectionOption> selectionOptions;
}
