package com.secondzip.backend.report.domain;

import com.secondzip.backend.report.enums.FraudType;
import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * report_fraud_types 테이블 한 행. (사기 유형 3종)
 *
 * 유형별 세부 결과는 report_fraud_detail_results 에 따로 있고
 * DetailResult 로 조회한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFraudType {

    private Long reportFraudTypeId;
    private Long analysisReportId;
    private FraudType fraudType;

    /** 세부 판정들을 개수 기반으로 집계한 유형 대표값. */
    private RiskLevel riskLevel;
}
