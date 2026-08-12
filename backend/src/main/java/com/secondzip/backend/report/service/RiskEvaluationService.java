package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.*;
import com.secondzip.backend.report.dto.external.BuildingData;
import com.secondzip.backend.report.dto.external.PriceData;
import com.secondzip.backend.report.dto.external.RegistryData;
import com.secondzip.backend.report.enums.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 위험도 판단 순서
 1. 필수점검 5개 판정 → 개수기반 집계로 대표값 1개
 2. 유형별 세부 9개 판정 (3개씩) → 개수기반 집계로 유형 대표값 3개
 3. [필수점검대표값, 유형1, 유형2, 유형3] 4개 중 최악값 = 전체 결과
 **/

@Service
public class RiskEvaluationService {

    // 전세가율 기준
    private static final double JEONSE_RATIO_CAUTION = 0.70;
    private static final double JEONSE_RATIO_DANGER = 0.80;
    // 선순위채권 부담 기준 (주택가격 90% × 60%)
    private static final double PRIORITY_DEBT_LIMIT_RATIO = 0.54;

    private static final int CHECK_DANGER_THRESHOLD = 3;   // 필수점검 5개 중 3개 이상 CAUTION → DANGER

    // 건물과 토지의 등기가 분리된 유형. 이 외에는 집합건물로 보고 토지 등기를 따로 보지 않는다.
    private static final List<String> SEPARATE_LAND_REGISTRY_TYPES =
            Arrays.asList("SINGLE_FAMILY", "MULTI_FAMILY");


    public RiskEvaluationResult evaluate(RegistryData registry, BuildingData building, PriceData price, Long deposit, String roadAddress) {

        String regionType = resolveRegionType(roadAddress);
        Long basePrice = pickBasePrice(price);
        // 근거에는 null을 그대로 남긴다. 확인하지 못한 값을 0으로 적으면
        // 리포트에 "근저당 0원"으로 표시되어 사용자가 안전하다고 오해한다.
        Long mortgageAmount = registry != null ? registry.getMortgageAmount() : null;

        // ===== 1-1. 필수점검 5개 =====
        List<CheckResult> checkResults = new ArrayList<>();
        checkResults.add(new CheckResult(
                CheckType.MORTGAGE_EXISTENCE,
                judgeMortgage(registry, price),
                evidence("mortgageAmount", mortgageAmount)
        ));

        checkResults.add(new CheckResult(
                CheckType.ILLEGAL_BUILDING,
                judgeIllegalBuilding(building),
                illegalBuildingEvidence(building)
        ));

        checkResults.add(new CheckResult(
                CheckType.BUILDING_USE,
                judgeBuildingUse(building),
                evidence("buildingUse", building != null ? building.getBuildingUse() : null)
        ));

        Map<String, Object> hugEvidence = new LinkedHashMap<>();
        hugEvidence.put("deposit", deposit);
        hugEvidence.put("mortgageAmount", mortgageAmount);
        hugEvidence.put("basePrice", basePrice);

        checkResults.add(new CheckResult(
                CheckType.HUG_GUARANTEE_ELIGIBILITY,
                judgeHugEligibility(building, price, registry, deposit, regionType),
                hugEvidence
        ));

        checkResults.add(new CheckResult(
                CheckType.RIGHTS_INFRINGEMENT,
                judgeRightsInfringement(registry),
                // 여기도 마찬가지. 확인 못한 것을 false로 적으면 "압류 없음"으로 읽힌다.
                evidence("hasSeizure", registry != null ? registry.getHasSeizure() : null)
        ));

        // ===== 1-2. 필수점검 최종 결과 (개수 기반) =====
        RiskLevel checkOverall = RiskLevel.aggregateByCount(
                applicableLevels(
                        checkResults.stream()
                                .map(c -> new Judgement(c.getRiskLevel(), c.getDataStatus()))
                                .toList()
                ),
                CHECK_DANGER_THRESHOLD
        );

        // ===== 2. 유형별 세부 9개 → 유형 3개 =====
        List<FraudTypeResult> fraudTypeResults = new ArrayList<>();
        fraudTypeResults.add(buildUnderwaterJeonse(registry, price, deposit));
        fraudTypeResults.add(buildRightsConcealment(registry, building));
        fraudTypeResults.add(buildTrustPropertyFraud(registry));

        // ===== 3. 전체 결과 = [필수점검최종, 유형1, 유형2, 유형3] 4개 중 최악값 =====
        List<RiskLevel> topLevels = new ArrayList<>();
        topLevels.add(checkOverall);
        fraudTypeResults.forEach(f -> topLevels.add(f.getRiskLevel()));
        RiskLevel overall = RiskLevel.worstOf(topLevels);

        return new RiskEvaluationResult(overall, checkResults, fraudTypeResults);
    }

    private Map<String, Object> illegalBuildingEvidence(BuildingData building) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        Boolean illegal = building != null
                ? building.getIsIllegalBuilding()
                : null;
        Boolean explicitVerified = building != null
                ? building.getIllegalBuildingVerified()
                : null;
        boolean verified = explicitVerified != null
                ? explicitVerified
                : illegal != null;

        evidence.put("isIllegalBuilding", illegal);
        evidence.put("isIllegalBuildingVerified", verified);
        evidence.put(
                "source",
                building != null ? building.getIllegalBuildingSource() : null
        );
        evidence.put(
                "violationByDocument",
                building != null && building.getViolationByDocument() != null
                        ? building.getViolationByDocument()
                        : Map.of()
        );
        return evidence;
    }

    // =========================================================
    // 필수점검 5개 판정 로직
    // =========================================================

    // 1. 근저당 유무
    /**
     * 실제로 보는 것: 등기상 채권최고액 합계를 기준가(실거래가 또는 공시가격)와 비교한 비율.
     *
     * [판정 로직]
     * - 근저당 금액을 확인하지 못함 -> CAUTION / UNVERIFIED
     * - 근저당 없음(0원) -> SAFE
     * - 기준가가 없어 비율 계산 불가 -> CAUTION / UNVERIFIED
     * - 비율 54% 초과 -> DANGER / 54% 이하 -> CAUTION (근저당은 존재)
     *
     * 확인하지 못한 것을 "없음(SAFE)"으로 판정하면 안 된다.
     */
    private Judgement judgeMortgage(RegistryData registry, PriceData price) {
        if (registry == null || registry.getMortgageAmount() == null) {
            return Judgement.unverified();
        }
        if (registry.getMortgageAmount() == 0) {
            return Judgement.verified(RiskLevel.SAFE); // 근저당 없음
        }

        Long basePrice = pickBasePrice(price);
        if (basePrice == null) {
            return Judgement.unverified(); // 근저당은 있는데 기준가가 없어 비율 계산 불가
        }

        double ratio = (double) registry.getMortgageAmount() / basePrice;
        return Judgement.verified(
                ratio > PRIORITY_DEBT_LIMIT_RATIO ? RiskLevel.DANGER : RiskLevel.CAUTION
        );
    }

    // 2. 위반건축물 여부
    /** 실제로 보는 것: 건축물대장의 위반건축물 표시 여부. */
    private Judgement judgeIllegalBuilding(BuildingData building) {
        if (building == null || building.getIsIllegalBuilding() == null) {
            return Judgement.unverified();
        }
        return Judgement.verified(
                building.getIsIllegalBuilding() ? RiskLevel.DANGER : RiskLevel.SAFE
        );
    }

    // 3. 건축물 용도
    /**
     * 실제로 보는 것: 건축물대장의 용도 문자열이 주거용인지.
     *
     * 주의 — 이 판정은 "고지된 용도와 실제 용도가 다른가"를 보지 않는다.
     * 비교할 고지 정보가 없기 때문이다. 유형2-B에서 같은 결과를 재사용하지만
     * 그 항목 이름({@code FALSE_BUILDING_USE_INFORMATION})이 뜻하는 '허위 안내'를
     * 실제로 검증하는 것은 아니다.
     *
     * [판정 로직]
     * - 정상 주거용 -> SAFE
     * - 업무용 오피스텔 등 -> CAUTION
     * - 근린생활시설·숙박시설 등 비주거용 -> DANGER
     */
    private Judgement judgeBuildingUse(BuildingData building) {
        if (building == null || building.getBuildingUse() == null) {
            return Judgement.unverified();
        }

        String use = building.getBuildingUse();
        // 주거 종류
        List<String> safeUses = Arrays.asList(
                "단독주택", "다가구주택", "다세대주택", "연립주택", "아파트", "주거용 오피스텔",
                "공동주택", "오피스텔"
        );

        for (String safeUse : safeUses) {
            if (use.contains(safeUse)) {
                if (use.contains("오피스텔") && !use.contains("주거")) {
                    return Judgement.verified(RiskLevel.CAUTION); // 업무용 오피스텔 등 추가 확인 필요
                }
                return Judgement.verified(RiskLevel.SAFE);
            }
        }

        // 근린생활시설, 숙박시설 등 비주거용
        return Judgement.verified(RiskLevel.DANGER);
    }

    // 4. HUG 보증보험 가입 가능 여부
    /**
     * 실제로 보는 것: 지역별 보증금 한도와 (보증금 + 선순위채권) 조건 두 가지.
     * 실제 HUG 심사가 아니라 금액 조건만 사전 점검한다.
     */
    private Judgement judgeHugEligibility(BuildingData building, PriceData price, RegistryData registry, Long deposit, String regionType) {
        if (regionType == null) return Judgement.unverified();
        if (building == null || building.getBuildingType() == null) {
            return Judgement.unverified();
        }
        String type = building.getBuildingType();
        // 다가구주택이나 집합건물(다세대, 연립 등)은 HUG 심사 특성상 무조건 CAUTION 고정
        if ("MULTI_FAMILY".equals(type) || "MULTI_HOUSEHOLD".equals(type)) {
            return Judgement.verified(RiskLevel.CAUTION);
        }

        Long basePrice = pickBasePrice(price);
        // 선순위채권을 확인하지 못하면 가입 가능 여부를 판정할 수 없다.
        // 이를 0으로 간주하면 실제로는 가입 불가인 매물이 SAFE로 나온다.
        if (basePrice == null || deposit == null
                || registry == null || registry.getMortgageAmount() == null) {
            return Judgement.unverified();
        }

        long mortgageAmount = registry.getMortgageAmount();

        // V = HUG 기준 주택가액 (집값의 90%)
        double v = basePrice * 0.9;

        // 지역별 보증금 한도 체크 (수도권 7억, 비수도권 5억)
        boolean isDepositValid = "METROPOLITAN".equals(regionType)
                ? deposit <= 700_000_000L
                : deposit <= 500_000_000L;

        // 조건 1: (D + S) <= V
        boolean condition1 = (deposit + mortgageAmount) <= v;
        // 조건 2: S <= (V * 0.60)
        boolean condition2 = mortgageAmount <= (v * 0.60);
        // 조건 중 하나라도 false -> DANGER
        if (!isDepositValid || !condition1 || !condition2) {
            return Judgement.verified(RiskLevel.DANGER); // 가입 불가 예상
        }

        return Judgement.verified(RiskLevel.SAFE);
    }

    // 5. 권리침해 여부
    /** 실제로 보는 것: 등기 갑구의 압류·가압류·경매개시결정 등 권리제한 표시. */
    private Judgement judgeRightsInfringement(RegistryData registry) {
        if (registry == null || registry.getHasSeizure() == null) {
            return Judgement.unverified();
        }
        return Judgement.verified(
                registry.getHasSeizure() ? RiskLevel.DANGER : RiskLevel.SAFE
        );
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
        List<DetailResult> details = Arrays.asList(
                new DetailResult(DetailType.HIGH_JEONSE_RATIO, judgeHighJeonseRatio(price, deposit)),
                new DetailResult(DetailType.PRIORITY_DEBT_BURDEN, judgePriorityDebtBurden(registry, price)),
                new DetailResult(DetailType.HUG_GUARANTEE_PRECHECK, judgeHugPrecheck(price, registry, deposit))
        );

        return new FraudTypeResult(
                FraudType.UNDERWATER_JEONSE,
                aggregateDetails(details),
                details
        );
    }


    // 1-A. 높은 전세가율 (명세서 공식: D / P)
    /**
     * 실제로 보는 것: 매매 기준가 대비 전세보증금 비율.
     *
     * [판정 로직]
     * - 80% 이상 -> DANGER / 70~80% -> CAUTION / 70% 미만 -> SAFE
     * - 가격 데이터 누락 -> CAUTION / UNVERIFIED
     */
    private Judgement judgeHighJeonseRatio(PriceData price, Long deposit) {
        Long basePrice = pickBasePrice(price);
        if (basePrice == null || deposit == null) return Judgement.unverified();

        double ratio = (double) deposit / basePrice;
        if (ratio >= JEONSE_RATIO_DANGER) return Judgement.verified(RiskLevel.DANGER);
        if (ratio >= JEONSE_RATIO_CAUTION) return Judgement.verified(RiskLevel.CAUTION);
        return Judgement.verified(RiskLevel.SAFE);
    }

    // 1-B. 선순위채권 부담 (명세서 공식: S / P)
    /**
     * 실제로 보는 것: 필수점검 1번과 동일한 계산(S / P, 기준 54%).
     * 깡통전세 유형 안에서도 이 신호를 반영하기 위해 의도적으로 중복 집계한다.
     */
    private Judgement judgePriorityDebtBurden(RegistryData registry, PriceData price) {
        Long basePrice = pickBasePrice(price);
        if (basePrice == null || registry == null || registry.getMortgageAmount() == null) {
            return Judgement.unverified(); // 계산 불가
        }

        long s = registry.getMortgageAmount();
        if (s == 0) return Judgement.verified(RiskLevel.SAFE); // 근저당 없음

        double ratio = (double) s / basePrice; // S / P
        return Judgement.verified(
                ratio > PRIORITY_DEBT_LIMIT_RATIO ? RiskLevel.DANGER : RiskLevel.CAUTION
        );
    }

    // 1-C. HUG 보증보험 사전점검 간이 로직 (명세서 공식: D + S <= P * 90%)
    /**
     * 실제로 보는 것: 금액 조건 하나만 본다((D + S) ≤ P × 90%).
     * 필수점검 4번은 여기에 지역별 한도와 조건2까지 더해 더 엄격하게 본다.
     */
    private Judgement judgeHugPrecheck(PriceData price, RegistryData registry, Long deposit) {
        Long basePrice = pickBasePrice(price);
        // 선순위채권을 모르면 (D + S)를 계산할 수 없다. 0으로 대체하지 않는다.
        if (basePrice == null || deposit == null
                || registry == null || registry.getMortgageAmount() == null) {
            return Judgement.unverified();
        }

        long s = registry.getMortgageAmount();
        double limit = basePrice * 0.90; // 보증한도 참고금액

        return Judgement.verified(
                (deposit + s) <= limit ? RiskLevel.SAFE : RiskLevel.DANGER
        );
    }

    // =========================================================
    // 유형 2: 권리은폐 (FALSE_INFORMATION_RIGHTS_CONCEALMENT)
    // =========================================================

    private FraudTypeResult buildRightsConcealment(RegistryData registry, BuildingData building) {
        List<DetailResult> details = Arrays.asList(
                new DetailResult(
                        DetailType.LAND_BUILDING_OWNERSHIP_MISMATCH,
                        judgeOwnershipMismatch(registry, building)
                ),
                // 2-B, 2-C는 필수점검 3번·5번과 같은 판정을 재사용한다.
                new DetailResult(
                        DetailType.FALSE_BUILDING_USE_INFORMATION,
                        judgeBuildingUse(building)
                ),
                new DetailResult(
                        DetailType.RIGHTS_INFRINGEMENT_CONCEALMENT,
                        judgeRightsInfringement(registry)
                )
        );

        return new FraudTypeResult(
                FraudType.FALSE_INFORMATION_RIGHTS_CONCEALMENT,
                aggregateDetails(details),
                details
        );
    }

    // 2-A. 건물·토지 소유관계 불일치
    /**
     * 실제로 보는 것: 건물 등기 소유자와 토지 등기 소유자가 같은 사람인지.
     *
     * 집합건물(아파트·다세대·오피스텔 등)은 해당 없음이다.
     * 대지권이 전유부분에 포함되어 토지 등기를 따로 확인할 필요가 없고,
     * 실제로 {@code landOwnerName}도 조회하지 않는다.
     * 예전에는 이 경우가 CAUTION으로 남아 아파트는 이 항목이 영원히
     * "확인 불가"로 표시됐다.
     *
     * [판정 로직]
     * - 집합건물 -> NOT_APPLICABLE (집계에서 제외)
     * - 소유자 일치 -> SAFE / 불일치 -> DANGER
     * - 건물·토지 소유자 정보 누락 -> CAUTION / UNVERIFIED
     */
    private Judgement judgeOwnershipMismatch(RegistryData registry, BuildingData building) {
        if (!hasSeparateLandRegistry(building)) {
            return Judgement.notApplicable();
        }
        if (registry == null
                || registry.getOwnerName() == null
                || registry.getLandOwnerName() == null) {
            return Judgement.unverified();
        }
        return Judgement.verified(
                registry.getOwnerName().equals(registry.getLandOwnerName())
                        ? RiskLevel.SAFE
                        : RiskLevel.DANGER
        );
    }

    /** 건물과 토지의 등기가 분리된 유형인지. 유형을 모르면 판단을 보류하고 검사 대상으로 둔다. */
    private boolean hasSeparateLandRegistry(BuildingData building) {
        if (building == null || building.getBuildingType() == null) {
            return true;
        }
        return SEPARATE_LAND_REGISTRY_TYPES.contains(building.getBuildingType());
    }

    // =========================================================
    // 유형 3: 신탁사기 (TRUST_PROPERTY_FRAUD)
    // =========================================================

    private FraudTypeResult buildTrustPropertyFraud(RegistryData registry) {
        List<DetailResult> details = Arrays.asList(
                new DetailResult(DetailType.TRUST_REGISTRATION_EXISTENCE, judgeTrustRegistration(registry)),
                new DetailResult(DetailType.REGISTERED_OWNER_VERIFICATION, judgeOwnerVerification(registry)),
                new DetailResult(DetailType.POST_TRUST_RIGHTS_INFRINGEMENT, judgePostTrustInfringement(registry))
        );

        return new FraudTypeResult(
                FraudType.TRUST_PROPERTY_FRAUD,
                aggregateDetails(details),
                details
        );
    }

    // 3-A. 신탁등기 존재 여부
    /** 실제로 보는 것: 등기 갑구에 신탁 표시가 있는지. */
    private Judgement judgeTrustRegistration(RegistryData registry) {
        if (registry == null || registry.getHasTrustRegistration() == null) {
            return Judgement.unverified();
        }
        return Judgement.verified(
                registry.getHasTrustRegistration() ? RiskLevel.DANGER : RiskLevel.SAFE
        );
    }

    // 3-B. 등기상 소유자 확인
    /** 실제로 보는 것: 소유자 이름에서 추론한 소유자 유형이 신탁회사인지. */
    private Judgement judgeOwnerVerification(RegistryData registry) {
        if (registry == null || registry.getOwnerType() == null) {
            return Judgement.unverified();
        }
        return Judgement.verified(
                "TRUST_COMPANY".equals(registry.getOwnerType()) ? RiskLevel.DANGER : RiskLevel.SAFE
        );
    }

    // 3-C. 신탁등기 이후 추가 권리침해 여부
    /**
     * 실제로 보는 것: 등기 변동 이력에서 마지막 신탁 표시 이후에 압류·경매 등이 붙었는지.
     *
     * [판정 로직]
     * - 신탁등기 없음 -> SAFE
     * - 신탁 + 추가 권리침해 있음 -> DANGER
     * - 신탁 + 추가 권리침해 없음 -> CAUTION (신탁관계 직접 확인 필요)
     * - 데이터 누락 -> CAUTION / UNVERIFIED
     */
    private Judgement judgePostTrustInfringement(RegistryData registry) {
        if (registry == null || registry.getHasTrustRegistration() == null) {
            return Judgement.unverified();
        }

        // 신탁등기 없음 -> 신탁 위험 미발견
        if (!registry.getHasTrustRegistration()) {
            return Judgement.verified(RiskLevel.SAFE);
        }

        if (registry.getHasPostTrustInfringement() == null) {
            return Judgement.unverified();
        }

        return Judgement.verified(
                registry.getHasPostTrustInfringement() ? RiskLevel.DANGER : RiskLevel.CAUTION
        );
    }


    // 수도권 판별
    private static final List<String> METROPOLITAN_KEYWORDS = Arrays.asList("서울", "경기", "인천");

    private String resolveRegionType(String roadAddress) {
        if (roadAddress == null) return null;  // 알 수 없음
        for (String keyword : METROPOLITAN_KEYWORDS) {
            if (roadAddress.startsWith(keyword)) return "METROPOLITAN";
        }
        return "NON_METROPOLITAN";
    }

    // =========================================================
    // 집계
    // =========================================================

    /**
     * 유형별 세부 3개를 유형 대표값으로 집계한다.
     *
     * <p>{@code NOT_APPLICABLE} 항목은 제외하고, <b>남은 항목이 전부 CAUTION일 때</b> DANGER로 올린다.
     * 임계값을 3으로 고정하면 해당 없는 항목이 있는 매물(예: 아파트)은
     * 아무리 나빠도 그 조건에 도달하지 못한다.
     */
    private RiskLevel aggregateDetails(List<DetailResult> details) {
        List<RiskLevel> applicable = details.stream()
                .filter(DetailResult::isApplicable)
                .map(DetailResult::getRiskLevel)
                .toList();
        if (applicable.isEmpty()) {
            return RiskLevel.SAFE;
        }
        return RiskLevel.aggregateByCount(applicable, applicable.size());
    }

    private List<RiskLevel> applicableLevels(List<Judgement> judgements) {
        return judgements.stream()
                .filter(Judgement::isApplicable)
                .map(Judgement::riskLevel)
                .toList();
    }

    // =========================================================
    // 공통 유틸
    // =========================================================

    /**
     * 기준가. 실거래가가 있으면 실거래가, 없으면 공시가격을 쓴다.
     */
    private Long pickBasePrice(PriceData price) {
        if (price == null) return null;
        Long basePrice = price.getRecentSalePrice() != null
                ? price.getRecentSalePrice()
                : price.getOfficialPrice();
        return (basePrice == null || basePrice <= 0L) ? null : basePrice;
    }

    // 판정 근거 저장
    private Map<String, Object> evidence(String key, Object value) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put(key, value);
        return evidence;
    }
}
