package com.secondzip.backend.report.dto;

import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DataStatus;
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
    private DataStatus dataStatus;
    private Map<String, Object> evidence;

    public CheckResult(
            CheckType checkType,
            Judgement judgement,
            Map<String, Object> evidence
    ) {
        this(checkType, judgement.riskLevel(), judgement.dataStatus(), evidence);
    }
}
