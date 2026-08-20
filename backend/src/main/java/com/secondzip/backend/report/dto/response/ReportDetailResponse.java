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

    /**
     * 건축물 유형. SINGLE_FAMILY / MULTI_FAMILY / APARTMENT / MULTI_HOUSEHOLD / OFFICETEL.
     *
     * 이 필드가 추가되기 전에 생성된 리포트는 null이다.
     */
    private String housingCategory;

    /** 신탁주택 여부. 등기에 신탁등기가 있거나 소유자가 신탁회사면 true. */
    private Boolean trustProperty;

    private List<CheckResultView> checkResults;
    private List<FraudTypeView> fraudTypes;

    // AI 추천 특약
    private List<SpecialTermView> specialTerms;
}