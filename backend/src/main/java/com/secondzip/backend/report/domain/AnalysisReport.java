package com.secondzip.backend.report.domain;

import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisReport {

    private Long analysisReportId;
    private Long accountId;
    private String requestId;
    private String roadAddress;
    private String detailAddress;
    private Long deposit;
    private RiskLevel riskLevel;
    private Boolean favorite;
    private LocalDateTime favoritedAt;

    /** 체크리스트 생성 시 유형별 항목을 붙이는 기준. 이 컬럼 추가 이전 리포트는 null. */
    private String housingCategory;

    /** 신탁주택 여부. TRUE 면 체크리스트에 TRUST_PROPERTY 항목이 추가된다. */
    private Boolean trustProperty;
}
