package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.*;
import com.secondzip.backend.report.dto.external.BuildingData;
import com.secondzip.backend.report.dto.external.PriceData;
import com.secondzip.backend.report.dto.external.RegistryData;
import com.secondzip.backend.report.enums.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
    /**
     * 공시가격을 시세에 준하는 금액으로 환산하는 배율.
     *
     * 공시가격은 시세의 60~70% 수준이라 그대로 기준가로 쓰면 전세가율과
     * 근저당 비율이 실제보다 크게 부풀려진다. HUG가 주택가격 산정에 쓰는
     * 140%를 그대로 적용해 기준가와 HUG 주택가액이 같은 척도를 쓰게 한다.
     */
    private static final BigDecimal OFFICIAL_PRICE_MULTIPLIER = new BigDecimal("1.4");
    private static final BigDecimal HUG_COLLATERAL_RATIO = new BigDecimal("0.9");
    private static final BigDecimal HUG_SENIOR_CLAIM_RATIO = new BigDecimal("0.6");

    private static final int CHECK_DANGER_THRESHOLD = 3;   // 필수점검 5개 중 3개 이상 CAUTION → DANGER

    // 건물과 토지의 등기가 분리된 유형. 이 외에는 집합건물로 보고 토지 등기를 따로 보지 않는다.
    private static final List<String> SEPARATE_LAND_REGISTRY_TYPES =
            Arrays.asList("SINGLE_FAMILY", "MULTI_FAMILY");
    private static final List<String> COLLECTIVE_REGISTRY_TYPES =
            Arrays.asList("APARTMENT", "MULTI_HOUSEHOLD", "OFFICETEL");
    private static final Set<String> KNOWN_OWNER_TYPES =
            Set.of("INDIVIDUAL", "CORPORATION", "TRUST_COMPANY");


    public RiskEvaluationResultDTO evaluate(RegistryData registry, BuildingData building, PriceData price, Long deposit, String roadAddress) {

        String regionType = resolveRegionType(roadAddress);
        Long basePrice = pickBasePrice(price);
        Long hugHousePrice = pickHugHousePrice(
                price,
                building != null ? building.getBuildingType() : null
        );
        // 근거에는 null을 그대로 남긴다. 확인하지 못한 값을 0으로 적으면
        // 리포트에 "근저당 0원"으로 표시되어 사용자가 안전하다고 오해한다.
        Long mortgageAmount = registry != null ? registry.getMortgageAmount() : null;

        // ===== 1-1. 필수점검 5개 =====
        List<CheckResultDTO> checkResultDTOS = new ArrayList<>();
        checkResultDTOS.add(new CheckResultDTO(
                CheckType.MORTGAGE_EXISTENCE,
                judgeMortgage(registry, price),
                evidence("mortgageAmount", mortgageAmount)
        ));

        checkResultDTOS.add(new CheckResultDTO(
                CheckType.ILLEGAL_BUILDING,
                judgeIllegalBuilding(building),
                illegalBuildingEvidence(building)
        ));

        checkResultDTOS.add(new CheckResultDTO(
                CheckType.BUILDING_USE,
                judgeBuildingUse(building),
                buildingUseEvidence(building)
        ));

        Map<String, Object> hugEvidence = new LinkedHashMap<>();
        hugEvidence.put("deposit", deposit);
        hugEvidence.put("mortgageAmount", mortgageAmount);
        hugEvidence.put("basePrice", basePrice);
        // 기준가가 실거래가인지 공시가격 환산액인지 리포트에서 구분할 수 있어야 한다.
        hugEvidence.put("basePriceSource", basePriceSource(price));
        hugEvidence.put("hugHousePrice", hugHousePrice);

        checkResultDTOS.add(new CheckResultDTO(
                CheckType.HUG_GUARANTEE_ELIGIBILITY,
                judgeHugEligibility(building, price, registry, deposit, regionType),
                hugEvidence
        ));

        checkResultDTOS.add(new CheckResultDTO(
                CheckType.RIGHTS_INFRINGEMENT,
                judgeRightsInfringement(registry),
                // 여기도 마찬가지. 확인 못한 것을 false로 적으면 "압류 없음"으로 읽힌다.
                evidence("hasSeizure", registry != null ? registry.getHasSeizure() : null)
        ));

        // ===== 1-2. 필수점검 최종 결과 (개수 기반) =====
        RiskLevel checkOverall = aggregateJudgements(
                checkResultDTOS.stream()
                        .map(c -> new JudgementDTO(c.getRiskLevel(), c.getDataStatus()))
                        .toList(),
                CHECK_DANGER_THRESHOLD
        );

        // ===== 2. 유형별 세부 9개 → 유형 3개 =====
        List<FraudTypeResultDTO> fraudTypeResultDTOS = new ArrayList<>();
        fraudTypeResultDTOS.add(buildUnderwaterJeonse(
                registry,
                building,
                price,
                deposit
        ));
        fraudTypeResultDTOS.add(buildRightsConcealment(registry, building));
        fraudTypeResultDTOS.add(buildTrustPropertyFraud(registry));

        // ===== 3. 전체 결과 = [필수점검최종, 유형1, 유형2, 유형3] 4개 중 최악값 =====
        List<RiskLevel> topLevels = new ArrayList<>();
        topLevels.add(checkOverall);
        fraudTypeResultDTOS.forEach(f -> topLevels.add(f.getRiskLevel()));
        RiskLevel overall = RiskLevel.worstOf(topLevels);

        return new RiskEvaluationResultDTO(overall, checkResultDTOS, fraudTypeResultDTOS);
    }

    private Map<String, Object> illegalBuildingEvidence(BuildingData building) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        Boolean illegal = building != null
                ? building.getIsIllegalBuilding()
                : null;
        boolean verified = isIllegalBuildingStatusVerified(building);

        evidence.put("isIllegalBuilding", illegal);
        evidence.put("isIllegalBuildingVerified", verified);
        // 키 이름은 API 명세(analysis-workflow-api-spec.md)와 일치시킨다.
        // evidence 안에서 "source"만으로는 무엇의 출처인지 알 수 없고,
        // 같은 맵의 isIllegalBuilding* 접두사와도 어긋난다.
        evidence.put(
                "illegalBuildingSource",
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
    private JudgementDTO judgeMortgage(RegistryData registry, PriceData price) {
        if (registry == null || registry.getMortgageAmount() == null) {
            return JudgementDTO.unverified();
        }
        if (registry.getMortgageAmount() < 0L) {
            return JudgementDTO.unverified();
        }
        if (registry.getMortgageAmount() == 0) {
            return JudgementDTO.verified(RiskLevel.SAFE); // 근저당 없음
        }

        Long basePrice = pickBasePrice(price);
        if (basePrice == null) {
            return JudgementDTO.unverified(); // 근저당은 있는데 기준가가 없어 비율 계산 불가
        }

        double ratio = (double) registry.getMortgageAmount() / basePrice;
        return JudgementDTO.verified(
                ratio > PRIORITY_DEBT_LIMIT_RATIO ? RiskLevel.DANGER : RiskLevel.CAUTION
        );
    }

    // 2. 위반건축물 여부
    /** 실제로 보는 것: 건축물대장의 위반건축물 표시 여부. */
    private JudgementDTO judgeIllegalBuilding(BuildingData building) {
        if (building == null
                || !isIllegalBuildingStatusVerified(building)
                || building.getIsIllegalBuilding() == null) {
            return JudgementDTO.unverified();
        }
        return JudgementDTO.verified(
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
     * - 계약 대상 호(전유부)가 정상 주거용 -> SAFE
     * - 전유부는 주거용인데 건물 전체에 비주거 용도가 섞여 있음 -> CAUTION
     * - 전유부가 업무용·비주거용 -> DANGER
     * - 주거/업무 구분이 없는 오피스텔·부속용도 -> CAUTION / UNVERIFIED
     *
     * 근생빌라 사기의 판별 기준은 <b>계약 대상 호의 용도</b>이지 건물 전체의
     * 주용도가 아니다. 1층이 근린생활시설인 정상 다세대·연립은 국내에 흔하므로,
     * 표제부에만 나타나는 비주거 표기를 그대로 DANGER로 확정하면 정상 매물이
     * 대량으로 위험하다고 표시된다. 대신 CAUTION으로 남겨 사용자가 해당 호의
     * 전유부 용도를 직접 확인하도록 유도한다.
     */
    private JudgementDTO judgeBuildingUse(BuildingData building) {
        if (building == null) {
            return JudgementDTO.unverified();
        }
        return switch (classifyBuildingUse(building.getBuildingUse())) {
            case RESIDENTIAL -> hasBuildingLevelNonResidentialUse(building)
                    ? JudgementDTO.verified(RiskLevel.CAUTION)
                    : JudgementDTO.verified(RiskLevel.SAFE);
            case NON_RESIDENTIAL -> JudgementDTO.verified(RiskLevel.DANGER);
            case UNKNOWN -> JudgementDTO.unverified();
        };
    }

    private boolean hasBuildingLevelNonResidentialUse(BuildingData building) {
        String uses = building != null
                ? building.getBuildingLevelNonResidentialUses()
                : null;
        return uses != null && !uses.isBlank();
    }

    private Map<String, Object> buildingUseEvidence(BuildingData building) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("buildingUse", building != null ? building.getBuildingUse() : null);
        // 표제부에만 있는 비주거 용도. 전유부 용도와 섞어 적으면 계약 대상 호가
        // 비주거인 것처럼 읽히므로 반드시 분리해 남긴다.
        evidence.put(
                "buildingLevelNonResidentialUses",
                building != null ? building.getBuildingLevelNonResidentialUses() : null
        );
        return evidence;
    }

    /** 비주거 표기가 주거 단어를 함께 포함하더라도 반드시 먼저 잡는다. */
    private BuildingUseKind classifyBuildingUse(String use) {
        if (use == null || use.isBlank()) {
            return BuildingUseKind.UNKNOWN;
        }

        String normalized = use.replaceAll("\\s+", "");

        // 오피스텔은 법정 주용도가 보통 '업무시설'이다. 따라서 명시적인
        // 주거/비주거 표기를 먼저 분리하지 않으면 실제 주거용 오피스텔도
        // 전부 비주거로 오판한다.
        if (normalized.contains("비주거")
                || normalized.contains("업무용")) {
            return BuildingUseKind.NON_RESIDENTIAL;
        }

        List<String> definitelyNonResidentialUses = Arrays.asList(
                "근린생활시설", "숙박시설", "생활숙박시설", "판매시설", "위락시설",
                "공장", "창고시설", "의료시설", "교육연구시설", "노유자시설",
                "종교시설", "자동차관련시설", "상가",
                "문화및집회시설", "운수시설", "운동시설", "수련시설",
                "위험물저장및처리시설", "동물및식물관련시설", "자원순환관련시설",
                "교정시설", "국방군사시설", "국방·군사시설", "국방ㆍ군사시설",
                "방송통신시설", "발전시설", "묘지관련시설", "관광휴게시설",
                "장례시설", "야영장시설"
        );
        if (definitelyNonResidentialUses.stream().anyMatch(normalized::contains)) {
            return BuildingUseKind.NON_RESIDENTIAL;
        }

        if (normalized.contains("주거용오피스텔")) {
            return BuildingUseKind.RESIDENTIAL;
        }
        // '오피스텔'만으로는 실제 주거용인지 업무용인지 확정할 수 없다.
        if (normalized.contains("오피스텔")) {
            return BuildingUseKind.UNKNOWN;
        }
        if (normalized.contains("업무시설")) {
            return BuildingUseKind.NON_RESIDENTIAL;
        }

        List<String> residentialUses = Arrays.asList(
                "단독주택", "다가구주택", "다세대주택", "연립주택", "아파트",
                "공동주택"
        );
        return residentialUses.stream().anyMatch(normalized::contains)
                ? BuildingUseKind.RESIDENTIAL
                : BuildingUseKind.UNKNOWN;
    }

    private enum BuildingUseKind {
        RESIDENTIAL,
        NON_RESIDENTIAL,
        UNKNOWN
    }

    // 4. HUG 보증보험 가입 가능 여부
    /** 현재 확보한 데이터로 HUG 반환보증의 가격·권리·용도 조건을 사전 점검한다. */
    private JudgementDTO judgeHugEligibility(BuildingData building, PriceData price, RegistryData registry, Long deposit, String regionType) {
        if (regionType == null) return JudgementDTO.unverified();
        if (building == null || building.getBuildingType() == null) {
            return JudgementDTO.unverified();
        }
        String type = building.getBuildingType();
        if (!isHugSupportedType(type) || deposit == null || deposit < 0L) {
            return JudgementDTO.unverified();
        }

        long depositLimit = "METROPOLITAN".equals(regionType)
                ? 700_000_000L
                : 500_000_000L;
        if (deposit > depositLimit) {
            return JudgementDTO.verified(RiskLevel.DANGER);
        }

        BuildingUseKind useKind = classifyBuildingUse(building.getBuildingUse());
        if (useKind == BuildingUseKind.NON_RESIDENTIAL) {
            return JudgementDTO.verified(RiskLevel.DANGER);
        }
        if (Boolean.TRUE.equals(building.getIsIllegalBuilding())) {
            return JudgementDTO.verified(RiskLevel.DANGER);
        }
        if (registry != null && Boolean.TRUE.equals(registry.getHasSeizure())) {
            return JudgementDTO.verified(RiskLevel.DANGER);
        }

        JudgementDTO ownership = hasSeparateLandRegistry(building)
                ? compareBuildingAndLandOwners(registry)
                : JudgementDTO.notApplicable();
        if (ownership.riskLevel() == RiskLevel.DANGER
                && ownership.dataStatus() == DataStatus.VERIFIED) {
            return ownership;
        }

        Long hugHousePrice = pickHugHousePrice(price, type);
        if (hugHousePrice == null || registry == null
                || registry.getMortgageAmount() == null
                || registry.getMortgageAmount() < 0L) {
            return JudgementDTO.unverified();
        }

        BigDecimal mortgage = BigDecimal.valueOf(registry.getMortgageAmount());
        BigDecimal collateralValue = BigDecimal.valueOf(hugHousePrice)
                .multiply(HUG_COLLATERAL_RATIO);
        BigDecimal depositAndSeniorClaims = BigDecimal.valueOf(deposit).add(mortgage);

        boolean depositAndClaimsFit = depositAndSeniorClaims.compareTo(collateralValue) <= 0;
        boolean seniorClaimsFit = mortgage.compareTo(
                collateralValue.multiply(HUG_SENIOR_CLAIM_RATIO)
        ) <= 0;
        if (!depositAndClaimsFit || !seniorClaimsFit) {
            return JudgementDTO.verified(RiskLevel.DANGER);
        }

        // 단독·다가구는 다른 세입자의 선순위 보증금까지 차감해야 한다.
        // 현재 데이터 모델에는 그 금액이 없으므로, 통과로 확정하지 않는다.
        boolean seniorTenantDepositsUnknown = "SINGLE_FAMILY".equals(type)
                || "MULTI_FAMILY".equals(type);
        boolean eligibilityDataUnknown = useKind == BuildingUseKind.UNKNOWN
                || !isIllegalBuildingStatusVerified(building)
                || building.getIsIllegalBuilding() == null
                || registry.getHasSeizure() == null
                || !Boolean.FALSE.equals(registry.getHasTrustRegistration())
                || !hasKnownBuildingOwner(registry)
                || registry.getOwnerType() == null
                || !KNOWN_OWNER_TYPES.contains(registry.getOwnerType())
                || "TRUST_COMPANY".equals(registry.getOwnerType())
                || Boolean.TRUE.equals(registry.getHasPostTrustInfringement())
                || ownership.dataStatus() == DataStatus.UNVERIFIED
                || seniorTenantDepositsUnknown;
        if (eligibilityDataUnknown) {
            return JudgementDTO.unverified();
        }
        return JudgementDTO.verified(RiskLevel.SAFE);
    }

    private boolean isIllegalBuildingStatusVerified(BuildingData building) {
        if (building == null) return false;
        Boolean explicit = building.getIllegalBuildingVerified();
        return explicit != null ? explicit : building.getIsIllegalBuilding() != null;
    }

    private boolean isHugSupportedType(String type) {
        return "SINGLE_FAMILY".equals(type)
                || "MULTI_FAMILY".equals(type)
                || "MULTI_HOUSEHOLD".equals(type)
                || "APARTMENT".equals(type)
                || "OFFICETEL".equals(type);
    }

    // 5. 권리침해 여부
    /** 실제로 보는 것: 등기 갑구의 압류·가압류·경매개시결정 등 권리제한 표시. */
    private JudgementDTO judgeRightsInfringement(RegistryData registry) {
        if (registry == null || registry.getHasSeizure() == null) {
            return JudgementDTO.unverified();
        }
        return JudgementDTO.verified(
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

    private FraudTypeResultDTO buildUnderwaterJeonse(
            RegistryData registry,
            BuildingData building,
            PriceData price,
            Long deposit
    ) {
        List<DetailResultDTO> details = Arrays.asList(
                new DetailResultDTO(
                        DetailType.HIGH_JEONSE_RATIO,
                        judgeHighJeonseRatio(building, price, deposit)
                ),
                new DetailResultDTO(DetailType.PRIORITY_DEBT_BURDEN, judgePriorityDebtBurden(registry, price)),
                new DetailResultDTO(
                        DetailType.HUG_GUARANTEE_PRECHECK,
                        judgeHugPrecheck(building, price, registry, deposit)
                )
        );

        return new FraudTypeResultDTO(
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
    private JudgementDTO judgeHighJeonseRatio(BuildingData building, PriceData price, Long deposit) {
        // 다가구 실거래가는 건물 전체 가격이어서 한 세대 보증금과 직접 나눌 수 없다.
        if (building != null && "MULTI_FAMILY".equals(building.getBuildingType())) {
            return JudgementDTO.unverified();
        }
        Long basePrice = pickBasePrice(price);
        if (basePrice == null || deposit == null || deposit < 0L) {
            return JudgementDTO.unverified();
        }

        double ratio = (double) deposit / basePrice;
        if (ratio >= JEONSE_RATIO_DANGER) return JudgementDTO.verified(RiskLevel.DANGER);
        if (ratio >= JEONSE_RATIO_CAUTION) return JudgementDTO.verified(RiskLevel.CAUTION);
        return JudgementDTO.verified(RiskLevel.SAFE);
    }

    // 1-B. 선순위채권 부담 (명세서 공식: S / P)
    /**
     * 실제로 보는 것: 필수점검 1번과 동일한 계산(S / P, 기준 54%).
     * 깡통전세 유형 안에서도 이 신호를 반영하기 위해 의도적으로 중복 집계한다.
     */
    private JudgementDTO judgePriorityDebtBurden(RegistryData registry, PriceData price) {
        Long basePrice = pickBasePrice(price);
        if (basePrice == null || registry == null
                || registry.getMortgageAmount() == null
                || registry.getMortgageAmount() < 0L) {
            return JudgementDTO.unverified(); // 계산 불가
        }

        long s = registry.getMortgageAmount();
        if (s == 0) return JudgementDTO.verified(RiskLevel.SAFE); // 근저당 없음

        double ratio = (double) s / basePrice; // S / P
        return JudgementDTO.verified(
                ratio > PRIORITY_DEBT_LIMIT_RATIO ? RiskLevel.DANGER : RiskLevel.CAUTION
        );
    }

    // 1-C. HUG 보증보험 사전점검 (명세서 공식: D + S <= 주택가액 × 90%)
    /**
     * 실제로 보는 것: 가격 조건 하나만 본다.
     *
     * 필수점검 4번({@link #judgeHugEligibility})은 여기에 지역별 보증금 한도,
     * 용도·위반건축물·권리침해·신탁·소유관계까지 더해 더 엄격하게 본다.
     * 그 판정을 여기서 그대로 재사용하면 "보증금이 수도권 한도 7억을 넘는다"는
     * 이유만으로 전세가율 30%인 매물이 깡통전세 DANGER로 확정된다.
     * 깡통전세 유형에서 물어야 할 것은 가격과 채무의 관계뿐이므로 분리한다.
     */
    private JudgementDTO judgeHugPrecheck(
            BuildingData building,
            PriceData price,
            RegistryData registry,
            Long deposit
    ) {
        Long hugHousePrice = pickHugHousePrice(
                price,
                building != null ? building.getBuildingType() : null
        );
        // 선순위채권을 모르면 (D + S)를 계산할 수 없다. 0으로 대체하지 않는다.
        if (hugHousePrice == null || deposit == null || deposit < 0L
                || registry == null || registry.getMortgageAmount() == null
                || registry.getMortgageAmount() < 0L) {
            return JudgementDTO.unverified();
        }

        BigDecimal limit = BigDecimal.valueOf(hugHousePrice)
                .multiply(HUG_COLLATERAL_RATIO);
        BigDecimal depositAndSeniorClaims = BigDecimal.valueOf(deposit)
                .add(BigDecimal.valueOf(registry.getMortgageAmount()));
        return JudgementDTO.verified(
                depositAndSeniorClaims.compareTo(limit) <= 0
                        ? RiskLevel.SAFE
                        : RiskLevel.DANGER
        );
    }

    // =========================================================
    // 유형 2: 권리은폐 (FALSE_INFORMATION_RIGHTS_CONCEALMENT)
    // =========================================================

    private FraudTypeResultDTO buildRightsConcealment(RegistryData registry, BuildingData building) {
        List<DetailResultDTO> details = Arrays.asList(
                new DetailResultDTO(
                        DetailType.LAND_BUILDING_OWNERSHIP_MISMATCH,
                        judgeOwnershipMismatch(registry, building)
                ),
                // 2-B, 2-C는 "고지·광고된 내용"과 등기·대장의 차이를 보는 항목이다.
                // 광고 원문을 입력받는 경로가 아직 없어 비교 대상 자체가 없으므로
                // 판정하지 않는다.
                //
                // 여기서 unverified()를 쓰면 안 된다. unverified()는 (CAUTION,
                // UNVERIFIED)라서 집계에 남고, aggregateJudgements가 SAFE를
                // CAUTION으로 끌어올린다. 그러면 이 두 항목만으로 유형2가 영원히
                // CAUTION에 묶이고, 전체 결과가 어떤 매물에서도 SAFE가 될 수 없다.
                // "확인에 실패한 항목"이 아니라 "이 리포트에 해당하지 않는 항목"이므로
                // 집계에서 제외되는 notApplicable()이 맞다.
                new DetailResultDTO(
                        DetailType.FALSE_BUILDING_USE_INFORMATION,
                        JudgementDTO.notApplicable()
                ),
                new DetailResultDTO(
                        DetailType.RIGHTS_INFRINGEMENT_CONCEALMENT,
                        JudgementDTO.notApplicable()
                )
        );

        return new FraudTypeResultDTO(
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
    private JudgementDTO judgeOwnershipMismatch(RegistryData registry, BuildingData building) {
        if (building == null || building.getBuildingType() == null
                || building.getBuildingType().isBlank()) {
            return JudgementDTO.unverified();
        }
        if (COLLECTIVE_REGISTRY_TYPES.contains(building.getBuildingType())) {
            return JudgementDTO.notApplicable();
        }
        if (!hasSeparateLandRegistry(building)) return JudgementDTO.unverified();
        return compareBuildingAndLandOwners(registry);
    }

    private JudgementDTO compareBuildingAndLandOwners(RegistryData registry) {
        if (registry == null) {
            return JudgementDTO.unverified();
        }

        List<String> buildingOwners = registry.getOwnerNames();
        List<String> landOwners = registry.getLandOwnerNames();
        if (buildingOwners != null && landOwners != null) {
            Set<String> normalizedBuildingOwners = normalizeOwnerNames(buildingOwners);
            Set<String> normalizedLandOwners = normalizeOwnerNames(landOwners);
            if (normalizedBuildingOwners.isEmpty() || normalizedLandOwners.isEmpty()) {
                return JudgementDTO.unverified();
            }
            return JudgementDTO.verified(
                    normalizedBuildingOwners.equals(normalizedLandOwners)
                            ? RiskLevel.SAFE
                            : RiskLevel.DANGER
            );
        }

        // 새 목록 필드가 양쪽 모두 미상인 구 데이터만 scalar 필드로 호환한다.
        if (buildingOwners != null || landOwners != null
                || registry.getOwnerName() == null
                || registry.getLandOwnerName() == null) {
            return JudgementDTO.unverified();
        }
        String buildingOwner = normalizeOwnerName(registry.getOwnerName());
        String landOwner = normalizeOwnerName(registry.getLandOwnerName());
        if (buildingOwner.isBlank() || landOwner.isBlank()) {
            return JudgementDTO.unverified();
        }
        return JudgementDTO.verified(
                buildingOwner.equals(landOwner) ? RiskLevel.SAFE : RiskLevel.DANGER
        );
    }

    private Set<String> normalizeOwnerNames(List<String> ownerNames) {
        Set<String> normalized = new TreeSet<>();
        for (String ownerName : ownerNames) {
            String value = normalizeOwnerName(ownerName);
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private boolean hasKnownBuildingOwner(RegistryData registry) {
        if (registry.getOwnerNames() != null) {
            return !normalizeOwnerNames(registry.getOwnerNames()).isEmpty();
        }
        return registry.getOwnerName() != null && !registry.getOwnerName().isBlank();
    }

    private String normalizeOwnerName(String ownerName) {
        return ownerName == null ? "" : ownerName.replaceAll("\\s+", "").trim();
    }

    /** 건물과 토지의 등기가 분리된 유형인지. */
    private boolean hasSeparateLandRegistry(BuildingData building) {
        if (building == null || building.getBuildingType() == null) {
            return true;
        }
        return SEPARATE_LAND_REGISTRY_TYPES.contains(building.getBuildingType());
    }

    // =========================================================
    // 유형 3: 신탁사기 (TRUST_PROPERTY_FRAUD)
    // =========================================================

    private FraudTypeResultDTO buildTrustPropertyFraud(RegistryData registry) {
        List<DetailResultDTO> details = Arrays.asList(
                new DetailResultDTO(DetailType.TRUST_REGISTRATION_EXISTENCE, judgeTrustRegistration(registry)),
                new DetailResultDTO(DetailType.REGISTERED_OWNER_VERIFICATION, judgeOwnerVerification(registry)),
                new DetailResultDTO(DetailType.POST_TRUST_RIGHTS_INFRINGEMENT, judgePostTrustInfringement(registry))
        );

        return new FraudTypeResultDTO(
                FraudType.TRUST_PROPERTY_FRAUD,
                aggregateDetails(details),
                details
        );
    }

    // 3-A. 신탁등기 존재 여부
    /** 실제로 보는 것: 등기 갑구에 신탁 표시가 있는지. */
    private JudgementDTO judgeTrustRegistration(RegistryData registry) {
        if (registry == null || registry.getHasTrustRegistration() == null) {
            return JudgementDTO.unverified();
        }
        if (!registry.getHasTrustRegistration()
                && "TRUST_COMPANY".equals(registry.getOwnerType())) {
            return JudgementDTO.unverified();
        }
        return JudgementDTO.verified(
                registry.getHasTrustRegistration() ? RiskLevel.DANGER : RiskLevel.SAFE
        );
    }

    // 3-B. 등기상 소유자 확인
    /** 실제로 보는 것: 소유자 이름에서 추론한 소유자 유형이 신탁회사인지. */
    private JudgementDTO judgeOwnerVerification(RegistryData registry) {
        if (registry == null || !hasKnownBuildingOwner(registry)
                || registry.getOwnerType() == null
                || !KNOWN_OWNER_TYPES.contains(registry.getOwnerType())) {
            return JudgementDTO.unverified();
        }
        return JudgementDTO.verified(
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
    private JudgementDTO judgePostTrustInfringement(RegistryData registry) {
        if (registry == null || registry.getHasTrustRegistration() == null) {
            return JudgementDTO.unverified();
        }

        // 신탁등기 없음 -> 신탁 위험 미발견
        if (!registry.getHasTrustRegistration()) {
            if (Boolean.TRUE.equals(registry.getHasPostTrustInfringement())) {
                return JudgementDTO.unverified();
            }
            return JudgementDTO.verified(RiskLevel.SAFE);
        }

        if (registry.getHasPostTrustInfringement() == null) {
            return JudgementDTO.unverified();
        }

        return JudgementDTO.verified(
                registry.getHasPostTrustInfringement() ? RiskLevel.DANGER : RiskLevel.CAUTION
        );
    }


    // 수도권 판별
    private static final List<String> METROPOLITAN_PREFIXES =
            Arrays.asList("서울", "서울특별시", "경기", "경기도", "인천", "인천광역시");
    private static final List<String> NON_METROPOLITAN_PREFIXES = Arrays.asList(
            "부산", "부산광역시", "대구", "대구광역시", "광주", "광주광역시",
            "대전", "대전광역시", "울산", "울산광역시", "세종", "세종특별자치시",
            "강원", "강원도", "강원특별자치도", "충북", "충청북도", "충남", "충청남도",
            "전북", "전라북도", "전북특별자치도", "전남", "전라남도",
            "경북", "경상북도", "경남", "경상남도", "제주", "제주특별자치도"
    );

    private String resolveRegionType(String roadAddress) {
        if (roadAddress == null || roadAddress.isBlank()) return null;
        String normalized = roadAddress.trim();
        // 지번주소나 시·도명만으로는 표준 도로명주소인지 보장할 수 없다.
        if (!normalized.matches(".*(?:대로|로|길)\\s+\\d+(?:-\\d+)?(?:\\s.*)?")) {
            return null;
        }
        for (String prefix : METROPOLITAN_PREFIXES) {
            if (startsWithRegion(normalized, prefix)) return "METROPOLITAN";
        }
        for (String prefix : NON_METROPOLITAN_PREFIXES) {
            if (startsWithRegion(normalized, prefix)) return "NON_METROPOLITAN";
        }
        // 도로 형태만 맞고 시·도를 확인하지 못한 문자열을 비수도권으로
        // 단정하면 수도권 한도(7억)를 5억으로 잘못 낮춘다.
        return null;
    }

    private boolean startsWithRegion(String address, String regionName) {
        return address.startsWith(regionName + " ");
    }

    // =========================================================
    // 집계
    // =========================================================

    /**
     * 유형별 세부 3개를 유형 대표값으로 집계한다.
     *
     * NOT_APPLICABLE 항목은 제외하고, 남은 항목이 전부 CAUTION일 때 DANGER로 올린다.
     * 임계값을 3으로 고정하면 해당 없는 항목이 있는 매물(예: 아파트)은
     * 아무리 나빠도 그 조건에 도달하지 못한다.
     */
    private RiskLevel aggregateDetails(List<DetailResultDTO> details) {
        List<JudgementDTO> judgementDTOS = details.stream()
                .map(detail -> new JudgementDTO(detail.getRiskLevel(), detail.getDataStatus()))
                .toList();
        long applicableCount = judgementDTOS.stream().filter(JudgementDTO::isApplicable).count();
        return aggregateJudgements(judgementDTOS, Math.max(1, (int) applicableCount));
    }

    /**
     * 확인 불가 CAUTION은 화면의 최소 CAUTION 표시에는 반영하되,
     * 실제 위험 CAUTION 개수에 포함해 DANGER로 승격하지 않는다.
     */
    private RiskLevel aggregateJudgements(List<JudgementDTO> judgementDTOS, int dangerThreshold) {
        List<RiskLevel> verifiedLevels = judgementDTOS.stream()
                .filter(JudgementDTO::isApplicable)
                .filter(judgementDTO -> judgementDTO.dataStatus() == DataStatus.VERIFIED)
                .map(JudgementDTO::riskLevel)
                .toList();
        RiskLevel verifiedAggregate = RiskLevel.aggregateByCount(
                verifiedLevels,
                dangerThreshold
        );
        boolean hasUnverified = judgementDTOS.stream()
                .anyMatch(judgementDTO -> judgementDTO.dataStatus() == DataStatus.UNVERIFIED);
        return verifiedAggregate == RiskLevel.SAFE && hasUnverified
                ? RiskLevel.CAUTION
                : verifiedAggregate;
    }

    // =========================================================
    // 공통 유틸
    // =========================================================

    /**
     * 기준가. 실거래가가 있으면 실거래가, 없으면 공시가격을 140% 환산해 쓴다.
     *
     * 공시가격을 환산 없이 쓰면 분모가 실제 시세보다 작아져 전세가율과
     * 근저당 비율이 함께 부풀려진다. 시세 5억(공시가 3.5억) 매물에 보증금
     * 3억이면 실제 60%인 전세가율이 86%로 계산되어 DANGER로 찍힌다.
     * 실거래가 매칭에 실패하는 경우가 드물지 않으므로 이 경로가 조용히
     * 대량 오판을 만들지 않도록 척도를 맞춘다.
     */
    private Long pickBasePrice(PriceData price) {
        if (price == null) return null;
        if (price.getRecentSalePrice() != null && price.getRecentSalePrice() > 0L) {
            return price.getRecentSalePrice();
        }
        return convertOfficialPrice(price.getOfficialPrice());
    }

    /** 기준가의 출처. 리포트에서 실거래가 기준인지 공시가 환산인지 구분하기 위한 값. */
    private String basePriceSource(PriceData price) {
        if (price == null) return null;
        if (price.getRecentSalePrice() != null && price.getRecentSalePrice() > 0L) {
            return "RECENT_SALE_PRICE";
        }
        return convertOfficialPrice(price.getOfficialPrice()) != null
                ? "OFFICIAL_PRICE_CONVERTED"
                : null;
    }

    private Long convertOfficialPrice(Long officialPrice) {
        if (officialPrice == null || officialPrice <= 0L) {
            return null;
        }
        try {
            return BigDecimal.valueOf(officialPrice)
                    .multiply(OFFICIAL_PRICE_MULTIPLIER)
                    .setScale(0, RoundingMode.DOWN)
                    .longValueExact();
        } catch (ArithmeticException e) {
            return null;
        }
    }

    /**
     * 현재 보유한 가격 두 개만으로 HUG의 유형별 적용순위를 보수적으로 재현한다.
     * 공시가격은 140% 환산 후 사용하고, 공시가격이 없을 때만 최근 매매가를 쓴다.
     */
    private Long pickHugHousePrice(PriceData price, String buildingType) {
        if (price == null || !isHugSupportedType(buildingType)) {
            return null;
        }

        Long convertedOfficialPrice = convertOfficialPrice(price.getOfficialPrice());
        if (convertedOfficialPrice != null) {
            return convertedOfficialPrice;
        }

        Long recentSalePrice = price.getRecentSalePrice();
        return recentSalePrice != null && recentSalePrice > 0L
                ? recentSalePrice
                : null;
    }

    // 판정 근거 저장
    private Map<String, Object> evidence(String key, Object value) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put(key, value);
        return evidence;
    }
}
