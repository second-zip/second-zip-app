package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AnalysisWorkflowState;
import com.secondzip.backend.report.dto.request.StartAnalysisAuthRequest;
import com.secondzip.backend.report.dto.request.ContinueAnalysisAuthRequest;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;

public interface BuildingRegisterGateway {
    BuildingRegisterGatewayResult start(
            AnalysisWorkflowState state,
            BuildingRegisterDocumentType documentType,
            StartAnalysisAuthRequest authRequest
    );

    BuildingRegisterGatewayResult continueRequest(
            AnalysisWorkflowState state,
            ContinueAnalysisAuthRequest request
    );
}
