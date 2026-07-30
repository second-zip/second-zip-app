package com.secondzip.backend.report.dto;

import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 유형별 세부사항
@Getter
@AllArgsConstructor
public class DetailResult {
    private DetailType detailType;
    private RiskLevel riskLevel;
}