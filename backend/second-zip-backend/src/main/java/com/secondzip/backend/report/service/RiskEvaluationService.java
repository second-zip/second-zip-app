package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.*;
import com.secondzip.backend.report.enums.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 위험도 판단 순서
 1. 필수점검 5개 판정
 2. 유형별 세부 9개 판정 (3개씩 묶어서 유형 3개로)
 3. 전체(8개) 최악값 집계
 4. 결과 반환
**/
@Service
public class RiskEvaluationService {

    // 전세가율 기준
    private static final double JEONSE_RATIO_CAUTION = 0.70;
    private static final double JEONSE_RATIO_DANGER = 0.80;
    // 선순위채권 부담 기준 (주택가격 90% × 60%)
    private static final double PRIORITY_DEBT_LIMIT_RATIO = 0.54;

    public RiskEvaluationResult evaluate(RegistryData registry, BuildingData building, PriceData price, Long deposit) {

        // ===== 1. 필수점검 5개 =====
        List<CheckResult> checkResults = new ArrayList<>();
        checkResults.add(new CheckResult(CheckType.MORTGAGE_EXISTENCE, judgeMortgage(registry, price)));
        checkResults.add(new CheckResult(CheckType.ILLEGAL_BUILDING, judgeIllegalBuilding(building)));
        checkResults.add(new CheckResult(CheckType.BUILDING_USE, judgeBuildingUse(building)));
        checkResults.add(new CheckResult(CheckType.HUG_GUARANTEE_ELIGIBILITY, judgeHugEligibility(building, price, deposit)));
        checkResults.add(new CheckResult(CheckType.RIGHTS_INFRINGEMENT, judgeRightsInfringement(registry)));

        // ===== 2. 유형별 세부 9개 → 유형 3개로 묶기 =====
        List<FraudTypeResult> fraudTypeResults = new ArrayList<>();
        fraudTypeResults.add(buildUnderwaterJeonse(registry, price, deposit));
        fraudTypeResults.add(buildRightsConcealment(registry, building));
        fraudTypeResults.add(buildTrustPropertyFraud(registry));

        // ===== 3. 전체 집계: 필수5 + 유형3(각 유형의 riskLevel) = 8개 중 최악값 =====
        List<RiskLevel> allLevels = new ArrayList<>();
        checkResults.forEach(c -> allLevels.add(c.getRiskLevel()));
        fraudTypeResults.forEach(f -> allLevels.add(f.getRiskLevel()));
        RiskLevel overall = RiskLevel.worstOf(allLevels);

        return new RiskEvaluationResult(overall, checkResults, fraudTypeResults);
    }

    // =========================================================
    // 필수점검 5개 판정 로직
    // =========================================================

    private RiskLevel judgeMortgage(RegistryData registry, PriceData price) {
        if (registry == null) return RiskLevel.CAUTION; // 확인 불가
        if (registry.getMortgageAmount() == null || registry.getMortgageAmount() == 0) return RiskLevel.SAFE;

        Long basePrice = pickBasePrice(price);
        if (basePrice == null) return RiskLevel.CAUTION; // 근저당은 있는데 기준가가 없어 비율 계산 불가

        double ratio = (double) registry.getMortgageAmount() / basePrice;
        return ratio > PRIORITY_DEBT_LIMIT_RATIO ? RiskLevel.DANGER : RiskLevel.CAUTION;
    }

    private RiskLevel judgeIllegalBuilding(BuildingData building) {
        if (building == null || building.getIsIllegalBuilding() == null) return RiskLevel.CAUTION;
        return building.getIsIllegalBuilding() ? RiskLevel.DANGER : RiskLevel.SAFE;
    }

    private RiskLevel judgeBuildingUse(BuildingData building) {
        if (building == null || building.getBuildingUse() == null) return RiskLevel.CAUTION;
        // 주거용으로 등재된 용도 목록. 그 외(근린생활시설 등)는 위험.
        List<String> residentialUses = Arrays.asList("공동주택", "단독주택", "다가구주택", "아파트", "다세대주택", "연립주택");
        return residentialUses.contains(building.getBuildingUse()) ? RiskLevel.SAFE : RiskLevel.DANGER;
    }

    private RiskLevel judgeHugEligibility(BuildingData building, PriceData price, Long deposit) {
        // 다가구주택은 HUG 심사 특성상 무조건 CAUTION 고정 (팀 확정 규칙)
        if (building != null && building.getBuildingType() == null) return RiskLevel.CAUTION;
        if (building != null && "MULTI_FAMILY".equals(building.getBuildingType())) return RiskLevel.CAUTION;

        Long basePrice = pickBasePrice(price);
        if (basePrice == null || deposit == null) return RiskLevel.CAUTION;

        double ratio = (double) deposit / basePrice;
        if (ratio > 1.0) return RiskLevel.DANGER;      // 보증금이 기준가를 초과 → 위험
        if (ratio > 0.90) return RiskLevel.CAUTION;    // HUG 통상 한도(90%) 근접
        return RiskLevel.SAFE;
    }

    private RiskLevel judgeRightsInfringement(RegistryData registry) {
        if (registry == null || registry.getHasSeizure() == null) return RiskLevel.CAUTION;
        return registry.getHasSeizure() ? RiskLevel.DANGER : RiskLevel.SAFE;
    }

    // =========================================================
    // 유형 1: 깡통전세 (UNDERWATER_JEONSE)
    // =========================================================

    private FraudTypeResult buildUnderwaterJeonse(RegistryData registry, PriceData price, Long deposit) {
        RiskLevel highJeonseRatio = judgeHighJeonseRatio(price, deposit);
        RiskLevel priorityDebtBurden = judgePriorityDebtBurden(registry, price, deposit);
        RiskLevel hugPrecheck = judgeHugEligibility(null, price, deposit); // 건물유형 무관, 금액만 재검토

        List<DetailResult> details = Arrays.asList(
                new DetailResult(DetailType.HIGH_JEONSE_RATIO, highJeonseRatio),
                new DetailResult(DetailType.PRIORITY_DEBT_BURDEN, priorityDebtBurden),
                new DetailResult(DetailType.HUG_GUARANTEE_PRECHECK, hugPrecheck)
        );
        RiskLevel worst = RiskLevel.worstOf(highJeonseRatio, priorityDebtBurden, hugPrecheck);
        return new FraudTypeResult(FraudType.UNDERWATER_JEONSE, worst, details);
    }


    private RiskLevel judgeHighJeonseRatio(PriceData price, Long deposit) {
        Long basePrice = pickBasePrice(price);
        if (basePrice == null || deposit == null) return RiskLevel.CAUTION; // 확인 불가

        double ratio = (double) deposit / basePrice;
        if (ratio >= JEONSE_RATIO_DANGER) return RiskLevel.DANGER;
        if (ratio >= JEONSE_RATIO_CAUTION) return RiskLevel.CAUTION;
        return RiskLevel.SAFE;
    }

    private RiskLevel judgePriorityDebtBurden(RegistryData registry, PriceData price, Long deposit) {
        Long basePrice = pickBasePrice(price);
        if (basePrice == null || deposit == null || registry == null || registry.getMortgageAmount() == null) {
            return RiskLevel.CAUTION;
        }
        double ratio = (double) (registry.getMortgageAmount() + deposit) / basePrice;
        return ratio > PRIORITY_DEBT_LIMIT_RATIO ? RiskLevel.DANGER : RiskLevel.SAFE;
    }

    // =========================================================
    // 유형 2: 권리은폐 (FALSE_INFORMATION_RIGHTS_CONCEALMENT)
    // =========================================================

    private FraudTypeResult buildRightsConcealment(RegistryData registry, BuildingData building) {
        RiskLevel ownershipMismatch = judgeOwnershipMismatch(registry);
        RiskLevel buildingUseMismatch = judgeBuildingUse(building); // 필수점검과 같은 로직 재사용 (스냅샷이라 중복 저장 OK)
        RiskLevel rightsConcealment = judgeRightsInfringement(registry); // 필수점검과 같은 로직 재사용

        List<DetailResult> details = Arrays.asList(
                new DetailResult(DetailType.LAND_BUILDING_OWNERSHIP_MISMATCH, ownershipMismatch),
                new DetailResult(DetailType.FALSE_BUILDING_USE_INFORMATION, buildingUseMismatch),
                new DetailResult(DetailType.RIGHTS_INFRINGEMENT_CONCEALMENT, rightsConcealment)
        );
        RiskLevel worst = RiskLevel.worstOf(ownershipMismatch, buildingUseMismatch, rightsConcealment);
        return new FraudTypeResult(FraudType.FALSE_INFORMATION_RIGHTS_CONCEALMENT, worst, details);
    }

    private RiskLevel judgeOwnershipMismatch(RegistryData registry) {
        if (registry == null || registry.getOwnerName() == null || registry.getLandOwnerName() == null) {
            return RiskLevel.CAUTION;
        }
        return registry.getOwnerName().equals(registry.getLandOwnerName()) ? RiskLevel.SAFE : RiskLevel.DANGER;
    }

    // =========================================================
    // 유형 3: 신탁사기 (TRUST_PROPERTY_FRAUD)
    // =========================================================

    private FraudTypeResult buildTrustPropertyFraud(RegistryData registry) {
        RiskLevel trustRegistration = judgeTrustRegistration(registry);
        RiskLevel ownerVerification = judgeOwnerVerification(registry);
        RiskLevel postTrustInfringement = judgePostTrustInfringement(registry);

        List<DetailResult> details = Arrays.asList(
                new DetailResult(DetailType.TRUST_REGISTRATION_EXISTENCE, trustRegistration),
                new DetailResult(DetailType.REGISTERED_OWNER_VERIFICATION, ownerVerification),
                new DetailResult(DetailType.POST_TRUST_RIGHTS_INFRINGEMENT, postTrustInfringement)
        );
        RiskLevel worst = RiskLevel.worstOf(trustRegistration, ownerVerification, postTrustInfringement);
        return new FraudTypeResult(FraudType.TRUST_PROPERTY_FRAUD, worst, details);
    }

    private RiskLevel judgeTrustRegistration(RegistryData registry) {
        if (registry == null || registry.getHasTrustRegistration() == null) return RiskLevel.CAUTION;
        return registry.getHasTrustRegistration() ? RiskLevel.DANGER : RiskLevel.SAFE;
    }

    private RiskLevel judgeOwnerVerification(RegistryData registry) {
        if (registry == null || registry.getOwnerType() == null) return RiskLevel.CAUTION;
        return "TRUST_COMPANY".equals(registry.getOwnerType()) ? RiskLevel.DANGER : RiskLevel.SAFE;
    }

    private RiskLevel judgePostTrustInfringement(RegistryData registry) {
        if (registry == null || registry.getHasPostTrustInfringement() == null) return RiskLevel.CAUTION;
        return registry.getHasPostTrustInfringement() ? RiskLevel.DANGER : RiskLevel.SAFE;
    }

    // =========================================================
    // 공통 유틸
    // =========================================================

    /** 전세가율 등 계산의 기준가로 쓸 가격. 실거래가 우선, 없으면 공시가격. 둘 다 없으면 null(확인불가). */
    private Long pickBasePrice(PriceData price) {
        if (price == null) return null;
        if (price.getRecentSalePrice() != null) return price.getRecentSalePrice();
        return price.getOfficialPrice();
    }
}
