package com.secondzip.backend.report.dto.response;

import com.secondzip.backend.report.dto.JudgementDTO;
import com.secondzip.backend.report.dto.RiskAggregation;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.RiskLevel;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public class ReportDetailResponse {
    private final Long analysisReportId;
    private final String roadAddress;
    private final String detailAddress;
    private final Long deposit;
    private final RiskLevel result;
    private final Boolean favorite;

    /**
     * 건축물 유형. SINGLE_FAMILY / MULTI_FAMILY / APARTMENT / MULTI_HOUSEHOLD / OFFICETEL.
     *
     * 이 필드가 추가되기 전에 생성된 리포트는 null이다.
     */
    private final String housingCategory;

    /** 신탁주택 여부. 등기에 신탁등기가 있거나 소유자가 신탁회사면 true. */
    private final Boolean trustProperty;

    private final List<CheckResultView> checkResults;
    private final List<FraudTypeView> fraudTypes;

    // AI 추천 특약
    private final List<SpecialTermView> specialTerms;

    /**
     * 최근 매매가(실거래가). 매칭된 거래가 없으면 null.
     *
     * 이전에는 HUG 보증 적격성 필수점검(checkResults 중 HUG_GUARANTEE_ELIGIBILITY)의
     * evidence.basePrice에만 일부 반영돼 있어, 그 체크가 어떤 이유로든 없으면 실거래가를
     * 확인할 방법이 없었다. 위험도 판정 자체와 무관하게 항상 확인할 수 있도록 최상위로
     * 올렸다. 이 필드가 추가되기 전에 생성된 리포트는 null이다.
     */
    private final Long recentSalePrice;

    /** 공시가격(환산 전 원본). 확인하지 못했으면 null. 이 필드 추가 전 리포트는 null */
    private final Long officialPrice;

    /**
     * 위험도 판정에 실제로 쓰인 기준가 출처.
     * "RECENT_SALE_PRICE"(실거래가) / "OFFICIAL_PRICE_CONVERTED"(공시가격 140% 환산)
     * / null(둘 다 확인 못함). recentSalePrice가 null이 아니어도 그 값이 위험도
     * 계산에 실제로 쓰였는지는 이 필드로 확인.
     */
    private final String basePriceSource;

    /**
     * 실거래가 필드가 추가되기 전 호출부(기존 테스트 등) 호환용 생성자.
     * 가격 정보 3개를 모두 null로 채움.
     */
    public ReportDetailResponse(
            Long analysisReportId,
            String roadAddress,
            String detailAddress,
            Long deposit,
            RiskLevel result,
            Boolean favorite,
            String housingCategory,
            Boolean trustProperty,
            List<CheckResultView> checkResults,
            List<FraudTypeView> fraudTypes,
            List<SpecialTermView> specialTerms
    ) {
        this(
                analysisReportId, roadAddress, detailAddress, deposit, result, favorite,
                housingCategory, trustProperty, checkResults, fraudTypes, specialTerms,
                null, null, null
        );
    }

    public ReportDetailResponse(
            Long analysisReportId,
            String roadAddress,
            String detailAddress,
            Long deposit,
            RiskLevel result,
            Boolean favorite,
            String housingCategory,
            Boolean trustProperty,
            List<CheckResultView> checkResults,
            List<FraudTypeView> fraudTypes,
            List<SpecialTermView> specialTerms,
            Long recentSalePrice,
            Long officialPrice,
            String basePriceSource
    ) {
        this.analysisReportId = analysisReportId;
        this.roadAddress = roadAddress;
        this.detailAddress = detailAddress;
        this.deposit = deposit;
        this.result = result;
        this.favorite = favorite;
        this.housingCategory = housingCategory;
        this.trustProperty = trustProperty;
        this.checkResults = checkResults;
        this.fraudTypes = fraudTypes;
        this.specialTerms = specialTerms;
        this.recentSalePrice = recentSalePrice;
        this.officialPrice = officialPrice;
        this.basePriceSource = basePriceSource;
    }

    /**
     * 필수점검 5개의 대표값.
     *
     * checkResults에서 파생되므로 저장 컬럼이 없음. 화면이 같은
     * 집계 규칙을 다시 구현하면 "확인 불가"와 "해당 없음"까지 위험으로 세게 되어
     * 근거가 하나도 없는 매물이 위험으로 표시. 규칙은 서버에만 둠.
     *
     * RiskAggregation 참고.
     */
    public RiskLevel getCheckResult() {
        if (checkResults == null || checkResults.isEmpty()) {
            return null;
        }
        return RiskAggregation.aggregateChecks(
                checkResults.stream()
                        .filter(Objects::nonNull)
                        .filter(view -> view.getResult() != null)
                        .map(view -> new JudgementDTO(
                                view.getResult(),
                                view.getDataStatus() != null
                                        ? view.getDataStatus()
                                        : DataStatus.VERIFIED
                        ))
                        .toList()
        );
    }

    /**
     * 전세사기 유형 3개의 대표값. 유형별 대표값 중 최악값이다.
     *
     * <p>{@link #fraudTypes}에서 파생된다. {@link #getCheckResult()}와 같은 이유로
     * 화면이 다시 계산하지 않도록 서버가 내려준다.
     */
    public RiskLevel getFraudResult() {
        if (fraudTypes == null || fraudTypes.isEmpty()) {
            return null;
        }
        List<RiskLevel> levels = fraudTypes.stream()
                .filter(Objects::nonNull)
                .map(FraudTypeView::getRiskLevel)
                .filter(Objects::nonNull)
                .toList();
        return levels.isEmpty() ? null : RiskLevel.worstOf(levels);
    }
}