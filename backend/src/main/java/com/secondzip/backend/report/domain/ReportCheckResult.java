package com.secondzip.backend.report.domain;

import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * report_check_results 테이블 한 행. (필수 점검 5항목)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCheckResult {

    private Long reportCheckResultId;
    private Long analysisReportId;
    private CheckType checkType;
    private RiskLevel riskLevel;

    /**
     * data_status 컬럼 원본 값.
     */
    private String dataStatus;

    /** evidence 컬럼 원본 JSON 문자열. */
    private String evidence;
}
