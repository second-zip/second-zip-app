package com.secondzip.backend.report.dto;

import com.secondzip.backend.report.dto.response.CheckResultView;
import com.secondzip.backend.report.dto.response.FraudTypeView;
import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SpecialTermGenerationContext {

    private Long analysisReportId;
    private Long deposit;
    private RiskLevel overallRiskLevel;
    private String housingType;
    private List<CheckResultView> checkResults;
    private List<FraudTypeView> fraudTypes;
}
