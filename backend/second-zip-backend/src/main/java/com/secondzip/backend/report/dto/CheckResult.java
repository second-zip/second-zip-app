package com.secondzip.backend.report.dto;

import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

// 필수점검
@Getter
@AllArgsConstructor
public class CheckResult {
    private CheckType checkType;
    private RiskLevel riskLevel;
    private Map<String, Object> evidence;
}