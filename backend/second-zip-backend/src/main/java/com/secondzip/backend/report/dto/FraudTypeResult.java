package com.secondzip.backend.report.dto;

import com.secondzip.backend.report.enums.FraudType;
import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FraudTypeResult {
    private FraudType fraudType;
    private RiskLevel riskLevel;        // 세부 3개 중 최악값
    private List<DetailResult> details; // 세부 3개
}