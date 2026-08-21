package com.secondzip.backend.report.dto;

import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

// 위험 분석 결과
@Getter
@AllArgsConstructor
public class RiskEvaluationResultDTO {
    private RiskLevel overallRiskLevel;             // 필수 5 + 유형 3 중 최악값
    private List<CheckResultDTO> checkResultDTOS;          // 필수 5개
    private List<FraudTypeResultDTO> fraudTypeResultDTOS;   // 유형 3개 (각각 세부 3개 포함)
}