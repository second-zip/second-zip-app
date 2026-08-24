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
public class CheckResultDTO {
    private CheckType checkType;
    private RiskLevel riskLevel;
    private DataStatus dataStatus;
    private Map<String, Object> evidence;

    public CheckResultDTO(
            CheckType checkType,
            JudgementDTO judgementDTO,
            Map<String, Object> evidence
    ) {
        this(checkType, judgementDTO.riskLevel(), judgementDTO.dataStatus(), evidence);
    }
}
