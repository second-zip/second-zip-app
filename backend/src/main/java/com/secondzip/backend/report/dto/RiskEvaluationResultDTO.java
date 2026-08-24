package com.secondzip.backend.report.dto;

import com.secondzip.backend.report.enums.RiskLevel;
import lombok.Getter;

import java.util.List;

// 위험 분석 결과
@Getter
public class RiskEvaluationResultDTO {
    private final RiskLevel overallRiskLevel;             // 필수 5 + 유형 3 중 최악값
    private final List<CheckResultDTO> checkResultDTOS;          // 필수 5개
    private final List<FraudTypeResultDTO> fraudTypeResultDTOS;   // 유형 3개 (각각 세부 3개 포함)

    /**
     * 실거래가 조회 결과. HUG_GUARANTEE_ELIGIBILITY 필수점검의 evidence에 이미
     * basePrice로 일부 반영돼 있었지만, 리포트 최상위에서 직접 확인하려면 그
     * 체크가 실행됐는지와 무관하게 값을 읽을 수 있어야 해서 별도로 노출한다.
     * 둘 다 null이면 실거래가/공시가격 모두 확인하지 못했다는 뜻이다.
     */
    private final Long recentSalePrice;   // 최근 매매가(실거래가). null = 매칭된 거래 없음
    private final Long officialPrice;     // 공시가격(환산 전 원본). null = 확인 불가
    private final String basePriceSource; // 위험도 판정에 쓴 기준가 출처: RECENT_SALE_PRICE / OFFICIAL_PRICE_CONVERTED / null(둘 다 없음)

    public RiskEvaluationResultDTO(
            RiskLevel overallRiskLevel,
            List<CheckResultDTO> checkResultDTOS,
            List<FraudTypeResultDTO> fraudTypeResultDTOS,
            Long recentSalePrice,
            Long officialPrice,
            String basePriceSource
    ) {
        this.overallRiskLevel = overallRiskLevel;
        this.checkResultDTOS = checkResultDTOS;
        this.fraudTypeResultDTOS = fraudTypeResultDTOS;
        this.recentSalePrice = recentSalePrice;
        this.officialPrice = officialPrice;
        this.basePriceSource = basePriceSource;
    }

    /**
     * 실거래가 필드가 추가되기 전 호출부(기존 테스트 등) 호환용.
     * 가격 정보 없이 필수점검·유형 결과만 검증하던 코드가 계속 컴파일되도록 남겨 둔다.
     */
    public RiskEvaluationResultDTO(
            RiskLevel overallRiskLevel,
            List<CheckResultDTO> checkResultDTOS,
            List<FraudTypeResultDTO> fraudTypeResultDTOS
    ) {
        this(overallRiskLevel, checkResultDTOS, fraudTypeResultDTOS, null, null, null);
    }
}