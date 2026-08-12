package com.secondzip.backend.report.dto.response;

import com.secondzip.backend.report.enums.FraudType;
import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FraudTypeView {
    private FraudType fraudType;
    private RiskLevel riskLevel;
    private List<DetailResultView> detailResults;
}