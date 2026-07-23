package com.secondzip.backend.report.dto;

import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RiskEvaluationResult {
    private RiskLevel overallRiskLevel;             // 필수 5 + 유형 3 중 최악값
    private List<CheckResult> checkResults;          // 필수 5개
    private List<FraudTypeResult> fraudTypeResults;   // 유형 3개 (각각 세부 3개 포함)
}