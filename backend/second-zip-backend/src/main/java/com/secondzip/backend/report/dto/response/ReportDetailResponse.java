package com.secondzip.backend.report.dto.response;

import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ReportDetailResponse {
    private Long analysisReportId;
    private String roadAddress;
    private String detailAddress;
    private Long deposit;
    private RiskLevel result;
    private Boolean favorite;
    private List<CheckResultView> checkResults;
    private List<FraudTypeView> fraudTypes;
}