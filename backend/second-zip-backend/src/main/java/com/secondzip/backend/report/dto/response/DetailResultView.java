package com.secondzip.backend.report.dto.response;

import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DetailResultView {
    private DetailType detailType;
    private RiskLevel result;
}