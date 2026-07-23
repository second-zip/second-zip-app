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

    public RiskEvaluationResult evaluate(RegistryData registry, BuildingData building, PriceData price, Long deposit, String roadAddress) {

        String regionType = resolveRegionType(roadAddress);
        // ===== 1. 필수점검 5개 =====
        List<CheckResult> checkResults = new ArrayList<>();
        checkResults.add(new CheckResult(CheckType.MORTGAGE_EXISTENCE, judgeMortgage(registry, price)));
        checkResults.add(new CheckResult(CheckType.ILLEGAL_BUILDING, judgeIllegalBuilding(building)));
        checkResults.add(new CheckResult(CheckType.BUILDING_USE, judgeBuildingUse(building)));
        checkResults.add(new CheckResult(CheckType.HUG_GUARANTEE_ELIGIBILITY, judgeHugEligibility(building, price, registry, deposit, regionType)));
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

    // 1. 근저당 유무
    private RiskLevel judgeMortgage(RegistryData registry, PriceData price) {
        if (registry == null) return RiskLevel.CAUTION; // 확인 불가
        if (registry.getMortgageAmount() == null || registry.getMortgageAmount() == 0) return RiskLevel.SAFE;

        Long basePrice = pickBasePrice(price);
        if (basePrice == null) return RiskLevel.CAUTION; // 근저당은 있는데 기준가가 없어 비율 계산 불가

        double ratio = (double) registry.getMortgageAmount() / basePrice;
        return ratio > PRIORITY_DEBT_LIMIT_RATIO ? RiskLevel.DANGER : RiskLevel.CAUTION;
    }

    // 2. 위반건축물 여부
    private RiskLevel judgeIllegalBuilding(BuildingData building) {
        if (building == null || building.getIsIllegalBuilding() == null) return RiskLevel.CAUTION;
        return building.getIsIllegalBuilding() ? RiskLevel.DANGER : RiskLevel.SAFE;
    }

    // 3. 건축물 용도 (정상 주거용 6가지: 단독, 다가구, 다세대, 연립, 아파트, 주거용 오피스텔)
    private RiskLevel judgeBuildingUse(BuildingData building) {
        if (building == null || building.getBuildingUse() == null) return RiskLevel.CAUTION;

        String use = building.getBuildingUse();

        // 주거 종류
        List<String> safeUses = Arrays.asList(
                "단독주택", "다가구주택", "다세대주택", "연립주택", "아파트", "주거용 오피스텔",
                "공동주택", "오피스텔"
        );

        for (String safeUse : safeUses) {
            if (use.contains(safeUse)) {
                if (use.contains("오피스텔") && !use.contains("주거")) {
                    return RiskLevel.CAUTION; // 업무용 오피스텔 등 추가 확인 필요
                }
                return RiskLevel.SAFE;
            }
        }

        // 근린생활시설, 숙박시설 등 비주거용
        return RiskLevel.DANGER;
    }

    // 4. HUG 보증보험 가입 가능 여부
    private RiskLevel judgeHugEligibility(BuildingData building, PriceData price, RegistryData registry, Long deposit, String regionType) {
        if (building != null && building.getBuildingType() != null) {
            String type = building.getBuildingType();
            // 다가구주택이나 집합건물(다세대, 연립 등)은 HUG 심사 특성상 무조건 CAUTION 고정
            if ("MULTI_FAMILY".equals(type) || "MULTI_HOUSEHOLD".equals(type)) {
                return RiskLevel.CAUTION;
            }
        }

        Long basePrice = pickBasePrice(price);
        if (basePrice == null || deposit == null) return RiskLevel.CAUTION;

        // 선순위채권(근저당)
        long mortgageAmount = (registry != null && registry.getMortgageAmount() != null)
                ? registry.getMortgageAmount() : 0L;

        // V = HUG 기준 주택가액 (집값의 90%)
        double v = basePrice * 0.90;

        // 지역별 보증금 한도 체크 (수도권 7억, 비수도권 5억)
        boolean isDepositValid = true;  // 한도가 넘으면 false, 한도가 안 넘으면 true
        if ("METROPOLITAN".equals(regionType)) {
            if (deposit > 700000000L) isDepositValid = false;
        } else {
            if (deposit > 500000000L) isDepositValid = false;
        }

        // 조건 1: (D + S) <= V
        boolean condition1 = (deposit + mortgageAmount) <= v;
        // 조건 2: S <= (V * 0.60)
        boolean condition2 = mortgageAmount <= (v * 0.60);
        // 조건 중 하나라도 false -> DANGER
        if (!isDepositValid || !condition1 || !condition2) {
            return RiskLevel.DANGER; // 가입 불가 예상
        }

        return RiskLevel.SAFE;
    }

    // 5. 권리침해 여부
    private RiskLevel judgeRightsInfringement(RegistryData registry) {
        // 데이터가 없거나 권리침해 여부가 확인되지 않는 경우 -> CAUTION (△ 최신 등기 확인 필요)
        if (registry == null || registry.getHasSeizure() == null) {
            return RiskLevel.CAUTION;
        }

        // 권리침해(경매, 압류, 가압류, 가처분, 가등기 등)가 하나라도 존재하면 DANGER (×)
        return registry.getHasSeizure() ? RiskLevel.DANGER : RiskLevel.SAFE;
    }

    // =========================================================
    // 유형 1: 깡통전세 (UNDERWATER_JEONSE)
    // =========================================================

    /**
     * 참고
     * H = HUG 인정 주택가격
     * V = HUG 기준 주택가액
     * D = 사용자가 입력한 전세보증금
     * S = 선순위채권 (근저당)
     * P = 기준 매매가격
     */

    private FraudTypeResult buildUnderwaterJeonse(RegistryData registry, PriceData price, Long deposit) {
        RiskLevel highJeonseRatio = judgeHighJeonseRatio(price, deposit);
        RiskLevel priorityDebtBurden = judgePriorityDebtBurden(registry, price);
        RiskLevel hugPrecheck = judgeHugPrecheck(price, registry, deposit);

        List<DetailResult> details = Arrays.asList(
                new DetailResult(DetailType.HIGH_JEONSE_RATIO, highJeonseRatio),
                new DetailResult(DetailType.PRIORITY_DEBT_BURDEN, priorityDebtBurden),
                new DetailResult(DetailType.HUG_GUARANTEE_PRECHECK, hugPrecheck)
        );
        RiskLevel worst = RiskLevel.worstOf(highJeonseRatio, priorityDebtBurden, hugPrecheck);
        return new FraudTypeResult(FraudType.UNDERWATER_JEONSE, worst, details);
    }


    // 1-A. 높은 전세가율 (명세서 공식: D / P)
    /**
     * 판단 기준: 매매가격 대비 전세보증금 비율(전세가율)을 확인하여 깡통전세 위험도를 판정합니다.
     * - D (보증금) = deposit
     * - P (기준가) = basePrice
     *
     * [판정 로직]
     * - 전세가율 80% 이상 -> DANGER (× 높은 전세가율)
     * - 전세가율 70% 이상 ~ 80% 미만 -> CAUTION (△ 주의 필요)
     * - 전세가율 70% 미만 -> SAFE (○ 상대적으로 낮음)
     * - 가격 데이터 누락 시 -> CAUTION (△ 계산 불가)
     */
    private RiskLevel judgeHighJeonseRatio(PriceData price, Long deposit) {
        Long basePrice = pickBasePrice(price);
        if (basePrice == null || deposit == null) return RiskLevel.CAUTION;

        double ratio = (double) deposit / basePrice;
        if (ratio >= JEONSE_RATIO_DANGER) return RiskLevel.DANGER;
        if (ratio >= JEONSE_RATIO_CAUTION) return RiskLevel.CAUTION;
        return RiskLevel.SAFE;
    }

    // 1-B. 선순위채권 부담 (명세서 공식: S / P)
    /**
     * 판단 기준: 집값 대비 앞선 빚(근저당 등)이 얼마나 되는지 확인하여 경매 시 보증금 미반환 위험을 판정합니다.
     * - S (선순위채권) = registry.getMortgageAmount()
     * - P (기준가) = basePrice
     * - 허용 참고기준(PRIORITY_DEBT_LIMIT_RATIO) = 54% (HUG 보증가액 90%의 60%)
     * * [판정 로직]
     * - 근저당권 없음 (S == 0) -> SAFE (○ 근저당 없음)
     * - 선순위채권 비율 54% 이하 -> CAUTION (△ 근저당 있음. 비율 내이지만 주의 필요)
     * - 선순위채권 비율 54% 초과 -> DANGER (× 선순위채권 과다)
     * - 가격/등기 데이터 누락 시 -> CAUTION (△ 계산 불가)
     */
    private RiskLevel judgePriorityDebtBurden(RegistryData registry, PriceData price) {
        Long basePrice = pickBasePrice(price);
        if (basePrice == null || registry == null || registry.getMortgageAmount() == null) {
            return RiskLevel.CAUTION; // 계산 불가
        }

        long s = registry.getMortgageAmount();
        if (s == 0) return RiskLevel.SAFE; // 근저당 없음 SAFE

        double ratio = (double) s / basePrice; // S / P
        // 54% 초과 -> DANGER, 54% 이하이면서 근저당이 존재 -> CAUTION
        return ratio > PRIORITY_DEBT_LIMIT_RATIO ? RiskLevel.DANGER : RiskLevel.CAUTION;
    }

    // 1-C. HUG 보증보험 사전점검 간이 로직 (명세서 공식: D + S <= P * 90%)
    /**
     * 판단 기준: 공식 HUG 주택가격 산정이 아닌, 국토부 실거래가 기반으로 HUG 가입 가능 금액조건만 사전 점검합니다.
     * - D (보증금) = deposit
     * - S (선순위채권) = registry.getMortgageAmount()
     * - P (기준가) = basePrice
     * - 보증한도 참고금액 (limit) = P * 90%
     * * [판정 로직]
     * - (보증금 + 선순위채권) <= 보증한도 참고금액 -> SAFE (○ 예상 금액 조건 충족)
     * - (보증금 + 선순위채권) > 보증한도 참고금액 -> DANGER (× 예상 금액 조건 초과)
     * - 가격/보증금 데이터 누락 시 -> CAUTION (△ 계산 불가)
     */
    private RiskLevel judgeHugPrecheck(PriceData price, RegistryData registry, Long deposit) {
        Long basePrice = pickBasePrice(price);
        if (basePrice == null || deposit == null) return RiskLevel.CAUTION;

        long s = (registry != null && registry.getMortgageAmount() != null) ? registry.getMortgageAmount() : 0L;
        double limit = basePrice * 0.90; // 보증한도 참고금액

        if ((deposit + s) <= limit) {
            return RiskLevel.SAFE; // 예상 금액 조건 충족 (○)
        } else {
            return RiskLevel.DANGER; // 예상 금액 조건 초과 (×)
        }
    }

    // =========================================================
    // 유형 2: 권리은폐 (FALSE_INFORMATION_RIGHTS_CONCEALMENT)
    // =========================================================

    private FraudTypeResult buildRightsConcealment(RegistryData registry, BuildingData building) {
        RiskLevel ownershipMismatch = judgeOwnershipMismatch(registry);
        // 2-B. 건축물 용도 허위 안내 (필수점검 3번 로직 재사용)
        RiskLevel buildingUseMismatch = judgeBuildingUse(building);
        // 2-C. 등기상 권리침해 은폐 (필수점검 5번 로직 재사용)
        RiskLevel rightsConcealment = judgeRightsInfringement(registry);

        List<DetailResult> details = Arrays.asList(
                new DetailResult(DetailType.LAND_BUILDING_OWNERSHIP_MISMATCH, ownershipMismatch),
                new DetailResult(DetailType.FALSE_BUILDING_USE_INFORMATION, buildingUseMismatch),
                new DetailResult(DetailType.RIGHTS_INFRINGEMENT_CONCEALMENT, rightsConcealment)
        );
        RiskLevel worst = RiskLevel.worstOf(ownershipMismatch, buildingUseMismatch, rightsConcealment);
        return new FraudTypeResult(FraudType.FALSE_INFORMATION_RIGHTS_CONCEALMENT, worst, details);
    }

    // 2-A. 건물·토지 소유관계 불일치
    /**
     * 판단 기준: 임대인이 건물과 토지를 모두 소유했는지(일치 여부) 확인하여 권리 분쟁 위험을 판정합니다.
     * - 건물 소유자: registry.getOwnerName()
     * - 토지 소유자: registry.getLandOwnerName()
     *
     * [판정 로직]
     * - 소유자 일치 -> SAFE (○ 소유관계 일치)
     * - 소유자 불일치 -> DANGER (× 소유관계 불일치)
     * - 토지/건물 정보 누락 시 -> CAUTION (△ 확인 불가)
     */
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

    // 3-A. 신탁등기 존재 여부
    /**
     * 판단 기준: 계약 대상 주택이 신탁회사에 넘어간(신탁등기) 상태인지 등기 갑구를 확인합니다.
     * * [판정 로직]
     * - 신탁등기 존재 -> DANGER (× 신탁등기 존재)
     * - 신탁등기 없음 -> SAFE (○ 신탁등기 미발견)
     * - 데이터 누락 시 -> CAUTION (△ 확인 불가)
     */
    private RiskLevel judgeTrustRegistration(RegistryData registry) {
        if (registry == null || registry.getHasTrustRegistration() == null) return RiskLevel.CAUTION;
        return registry.getHasTrustRegistration() ? RiskLevel.DANGER : RiskLevel.SAFE;
    }

    // 3-B. 등기상 소유자 확인
    /**
     * 판단 기준: 현재 등기상 소유자가 개인/일반법인인지 신탁회사인지 확인하여 임대 권한을 점검합니다.
     * * [판정 로직]
     * - 소유자 타입이 신탁회사("TRUST_COMPANY") -> DANGER (× 신탁회사 소유)
     * - 일반 개인/법인 -> SAFE (○ 일반 소유관계)
     * - 데이터 누락 시 -> CAUTION (△ 소유자 확인 불가)
     */
    private RiskLevel judgeOwnerVerification(RegistryData registry) {
        if (registry == null || registry.getOwnerType() == null) return RiskLevel.CAUTION;
        return "TRUST_COMPANY".equals(registry.getOwnerType()) ? RiskLevel.DANGER : RiskLevel.SAFE;
    }

    // 3-C. 신탁등기 이후 추가 권리침해 여부
    /**
     * 판단 기준: 신탁등기가 설정된 이후 경매, 압류, 가압류 등 추가적인 권리 제한이 발생했는지 점검합니다.
     * * [판정 로직]
     * - 신탁등기 없음 -> SAFE (○ 신탁 위험 미발견)
     * - 신탁등기 존재 + 추가 권리침해 있음 -> DANGER (× 신탁 이후 추가 권리침해 존재)
     * - 신탁등기 존재 + 추가 권리침해 없음 -> CAUTION (△ 신탁관계 직접 확인 필요)
     * - 데이터 누락 시 -> CAUTION (△ 최신 등기 확인 필요)
     */
    private RiskLevel judgePostTrustInfringement(RegistryData registry) {
        if (registry == null || registry.getHasTrustRegistration() == null || registry.getHasPostTrustInfringement() == null) {
            return RiskLevel.CAUTION;
        }

        // 신탁등기 없음 -> 신탁 위험 미발견 (○)
        if (!registry.getHasTrustRegistration()) {
            return RiskLevel.SAFE;
        }

        // 신탁등기가 있는 상태에서
        // 추가 권리침해 존재 -> DANGER (×)
        // 추가 권리침해 없음 -> CAUTION (△ 신탁관계 직접 확인 필요)
        return registry.getHasPostTrustInfringement() ? RiskLevel.DANGER : RiskLevel.CAUTION;
    }

    // =========================================================
    // 공통 유틸
    // =========================================================

    // 수도권 판별
    private static final List<String> METROPOLITAN_KEYWORDS = Arrays.asList("서울", "경기", "인천");

    private String resolveRegionType(String roadAddress) {
        if (roadAddress == null) return null;  // 알 수 없음
        for (String keyword : METROPOLITAN_KEYWORDS) {
            if (roadAddress.startsWith(keyword)) return "METROPOLITAN";
        }
        return "NON_METROPOLITAN";
    }


    private Long pickBasePrice(PriceData price) {
        if (price == null) return null;
        if (price.getRecentSalePrice() != null) return price.getRecentSalePrice();
        return price.getOfficialPrice();
    }
}