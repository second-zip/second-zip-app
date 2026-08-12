package com.secondzip.backend.report.dto.response;

import com.secondzip.backend.report.enums.AnalysisNextAction;
import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import com.secondzip.backend.report.dto.AnalysisSelectionOption;

import java.util.List;

@Getter
@AllArgsConstructor
public class AnalysisAuthResponse {
    private final String requestId;
    private final AnalysisRequestStatus status;
    private final BuildingRegisterDocumentType pendingDocument;
    private final AnalysisNextAction nextAction;
    private final List<AnalysisSelectionOption> selectionOptions;
    private final String captchaImage;
    private final long expiresAtEpochMillis;
}
