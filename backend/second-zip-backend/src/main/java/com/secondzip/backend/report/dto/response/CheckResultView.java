package com.secondzip.backend.report.dto.response;

import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class CheckResultView {
    private CheckType checkType;
    private RiskLevel result;
}