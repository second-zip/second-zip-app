package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.*;
import com.secondzip.backend.report.dto.external.BuildingData;
import com.secondzip.backend.report.dto.external.PriceData;
import com.secondzip.backend.report.dto.external.RegistryData;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.FraudType;
import com.secondzip.backend.report.enums.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RiskEvaluationServiceTest {

    private final RiskEvaluationService service = new RiskEvaluationService();

    @Test
    @DisplayName("위반건축물로 확인되면 위험으로 판정한다")
    void illegalBuildingIsDanger() {
        CheckResultDTO result = evaluateIllegalBuilding(true);

        assertEquals(RiskLevel.DANGER, result.getRiskLevel());
        assertEquals(true, result.getEvidence().get("isIllegalBuildingVerified"));
    }

    @Test
    @DisplayName("위반건축물이 아닌 것으로 확인되면 안전으로 판정한다")
    void verifiedLegalBuildingIsSafe() {
        CheckResultDTO result = evaluateIllegalBuilding(false);

        assertEquals(RiskLevel.SAFE, result.getRiskLevel());
        assertEquals(true, result.getEvidence().get("isIllegalBuildingVerified"));
    }

    @Test
    @DisplayName("위반건축물 정보를 제공받지 못하면 주의로 판정한다")
    void unknownIllegalBuildingStatusIsCaution() {
        CheckResultDTO result = evaluateIllegalBuilding(null);

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(false, result.getEvidence().get("isIllegalBuildingVerified"));
        assertEquals(null, result.getEvidence().get("isIllegalBuilding"));
    }

    @Test
    void explicitUnverifiedFlagPreventsSafeIllegalBuildingResult() {
        BuildingData building = new BuildingData();
        building.setIsIllegalBuilding(false);
        building.setIllegalBuildingVerified(false);
        building.setBuildingUse("공동주택");
        building.setBuildingType("APARTMENT");

        CheckResultDTO result = service.evaluate(
                        null, building, null, 100_000_000L, "서울특별시 강남구"
                ).getCheckResultDTOS().stream()
                .filter(item -> item.getCheckType() == CheckType.ILLEGAL_BUILDING)
                .findFirst()
                .orElseThrow();

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(DataStatus.UNVERIFIED, result.getDataStatus());
        assertEquals(false, result.getEvidence().get("isIllegalBuildingVerified"));
    }

    @Test
    void preservesCodefViolationEvidence() {
        BuildingData building = new BuildingData();
        building.setIsIllegalBuilding(false);
        building.setIllegalBuildingVerified(true);
        building.setIllegalBuildingSource("CODEF_BUILDING_REGISTER");
        LinkedHashMap<String, Boolean> byDocument = new LinkedHashMap<>();
        byDocument.put("COLLECTIVE_TITLE", false);
        byDocument.put("COLLECTIVE_EXCLUSIVE", false);
        building.setViolationByDocument(byDocument);

        RiskEvaluationResultDTO evaluation = service.evaluate(
                null,
                building,
                null,
                100_000_000L,
                "서울특별시 강남구"
        );
        CheckResultDTO result = evaluation.getCheckResultDTOS().stream()
                .filter(item ->
                        item.getCheckType() == CheckType.ILLEGAL_BUILDING
                )
                .findFirst()
                .orElseThrow();

        assertEquals(
                "CODEF_BUILDING_REGISTER",
                result.getEvidence().get("illegalBuildingSource")
        );
        assertEquals(
                byDocument,
                result.getEvidence().get("violationByDocument")
        );
    }

    @Test
    @DisplayName("근저당 금액을 확인하지 못하면 안전이 아니라 주의로 판정한다")
    void unknownMortgageAmountIsCautionNotSafe() {
        RegistryData registry = new RegistryData();
        registry.setMortgageAmount(null); // 확인 불가

        CheckResultDTO result = evaluateMortgage(registry, price(1_000_000_000L));

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertNull(
                result.getEvidence().get("mortgageAmount"),
                "확인하지 못한 값을 0으로 적으면 리포트에 '근저당 0원'으로 표시된다"
        );
    }

    @Test
    @DisplayName("근저당이 없는 것으로 확인되면 안전으로 판정한다")
    void confirmedZeroMortgageIsSafe() {
        RegistryData registry = new RegistryData();
        registry.setMortgageAmount(0L);

        CheckResultDTO result = evaluateMortgage(registry, price(1_000_000_000L));

        assertEquals(RiskLevel.SAFE, result.getRiskLevel());
        assertEquals(0L, result.getEvidence().get("mortgageAmount"));
    }

    @Test
    @DisplayName("기준가가 0이면 비율을 계산하지 않고 주의로 판정한다")
    void zeroBasePriceDoesNotProduceInfiniteRatio() {
        RegistryData registry = new RegistryData();
        registry.setMortgageAmount(100_000_000L);

        CheckResultDTO result = evaluateMortgage(registry, price(0L));

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
    }

    @Test
    @DisplayName("확인 불가는 위험도가 아니라 데이터 상태로 구분된다")
    void unverifiedIsDistinguishableFromRealRisk() {
        CheckResultDTO unknown = evaluateMortgage(new RegistryData(), price(1_000_000_000L));

        assertEquals(RiskLevel.CAUTION, unknown.getRiskLevel());
        assertEquals(DataStatus.UNVERIFIED, unknown.getDataStatus());

        RegistryData withMortgage = new RegistryData();
        withMortgage.setMortgageAmount(100_000_000L);
        CheckResultDTO real = evaluateMortgage(withMortgage, price(1_000_000_000L));

        assertEquals(RiskLevel.CAUTION, real.getRiskLevel());
        assertEquals(
                DataStatus.VERIFIED,
                real.getDataStatus(),
                "같은 CAUTION이라도 실제 근저당이 있는 경우는 VERIFIED여야 한다"
        );
    }

    @Test
    @DisplayName("집합건물은 토지 소유자 대조가 해당 없음으로 빠진다")
    void collectiveBuildingSkipsLandOwnerComparison() {
        DetailResultDTO ownership = ownershipMismatchOf("APARTMENT");

        assertEquals(DataStatus.NOT_APPLICABLE, ownership.getDataStatus());
        assertEquals(
                RiskLevel.CAUTION,
                ownership.getRiskLevel(),
                "dataStatus를 읽지 않는 클라이언트에서 '해당 없음'이 '안전'으로 "
                        + "표시되지 않도록 기존 화면과 같은 값을 유지한다"
        );
    }

    @Test
    @DisplayName("단독주택은 토지 소유자 정보가 없으면 확인 불가로 남는다")
    void singleFamilyWithoutLandOwnerIsUnverified() {
        DetailResultDTO ownership = ownershipMismatchOf("SINGLE_FAMILY");

        assertEquals(DataStatus.UNVERIFIED, ownership.getDataStatus());
        assertEquals(RiskLevel.CAUTION, ownership.getRiskLevel());
    }

    @Test
    @DisplayName("미확인 CAUTION은 위험 개수에 세어 DANGER로 승격하지 않는다")
    void unverifiedDetailsDoNotEscalateToDanger() {
        // 아파트 + 등기 정보 없음 → 유형1의 세부 3개가 모두 확인 불가지만
        // 그 CAUTION이 개수로 세어져 DANGER로 올라가지는 않는다.
        BuildingData building = new BuildingData();
        building.setBuildingType("APARTMENT");

        FraudTypeResultDTO type1 = service.evaluate(
                        null, building, price(1_000_000_000L), 100_000_000L, "서울특별시 강남구"
                ).getFraudTypeResultDTOS().stream()
                .filter(f -> f.getFraudType() == FraudType.UNDERWATER_JEONSE)
                .findFirst()
                .orElseThrow();

        assertEquals(
                RiskLevel.CAUTION,
                type1.getRiskLevel(),
                "등기를 확인하지 못한 것은 위험 확정이 아니다."
        );
    }

    @Test
    @DisplayName("건축물 용도·권리침해 은폐 세부항목은 필수점검 3·5번 판정을 그대로 재사용한다")
    void concealmentDetailsReuseRequiredCheckResults() {
        BuildingData building = new BuildingData();
        building.setBuildingType("APARTMENT");
        building.setBuildingUse("아파트");
        RegistryData registry = new RegistryData();
        registry.setHasSeizure(true);

        FraudTypeResultDTO result = service.evaluate(
                        registry, building, price(1_000_000_000L), 100_000_000L,
                        "서울특별시 강남구 테헤란로 11"
                ).getFraudTypeResultDTOS().stream()
                .filter(f -> f.getFraudType() == FraudType.FALSE_INFORMATION_RIGHTS_CONCEALMENT)
                .findFirst()
                .orElseThrow();

        DetailResultDTO buildingUseConcealment = result.getDetails().stream()
                .filter(d -> d.getDetailType() == DetailType.FALSE_BUILDING_USE_INFORMATION)
                .findFirst()
                .orElseThrow();
        DetailResultDTO rightsConcealment = result.getDetails().stream()
                .filter(d -> d.getDetailType() == DetailType.RIGHTS_INFRINGEMENT_CONCEALMENT)
                .findFirst()
                .orElseThrow();

        assertEquals(
                RiskLevel.SAFE,
                buildingUseConcealment.getRiskLevel(),
                "건축물 용도가 정상 주거용이면 필수점검 3번과 같은 판정을 반환해야 한다"
        );
        assertEquals(DataStatus.VERIFIED, buildingUseConcealment.getDataStatus());
        assertEquals(
                RiskLevel.DANGER,
                rightsConcealment.getRiskLevel(),
                "등기상 압류가 있으면 필수점검 5번과 같은 판정을 반환해야 한다 — "
                        + "notApplicable()로 빼면 실제 압류가 권리은폐 유형에서 사라진다"
        );
        assertEquals(DataStatus.VERIFIED, rightsConcealment.getDataStatus());
        assertEquals(
                RiskLevel.DANGER,
                result.getRiskLevel(),
                "권리침해가 확인되면 권리은폐 유형 대표값도 DANGER여야 한다"
        );
    }

    @Test
    @DisplayName("모든 데이터가 확보되고 위험 신호가 없으면 리포트 전체 결과가 SAFE다")
    void cleanApartmentIsSafeOverall() {
        RiskEvaluationResultDTO evaluation = service.evaluate(
                cleanRegistry(),
                cleanApartment(),
                cleanPrice(),
                300_000_000L,
                "서울특별시 강남구 테헤란로 152"
        );

        assertEquals(
                RiskLevel.SAFE,
                evaluation.getOverallRiskLevel(),
                "항상 CAUTION 이상으로 고정되는 항목이 하나라도 생기면 "
                        + "어떤 매물도 안전으로 안내할 수 없게 된다"
        );
    }

    @Test
    @DisplayName("1층이 근린생활시설인 정상 다세대는 위험이 아니라 주의로 남긴다")
    void residentialUnitInMixedUseBuildingIsCautionNotDanger() {
        BuildingData building = cleanApartment();
        building.setBuildingType("MULTI_HOUSEHOLD");
        building.setBuildingUse("다세대주택");
        building.setBuildingLevelNonResidentialUses("제2종근린생활시설");

        CheckResultDTO result = service.evaluate(
                        cleanRegistry(), building, cleanPrice(), 300_000_000L,
                        "서울특별시 강남구 테헤란로 152"
                ).getCheckResultDTOS().stream()
                .filter(item -> item.getCheckType() == CheckType.BUILDING_USE)
                .findFirst()
                .orElseThrow();

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
        assertEquals("연립·다세대주택", result.getEvidence().get("buildingUse"));
        assertEquals("다세대주택", result.getEvidence().get("buildingUseRaw"));
        assertEquals(
                "제2종근린생활시설",
                result.getEvidence().get("buildingLevelNonResidentialUses")
        );
    }

    @Test
    @DisplayName("업무용이 해당 호 원문에 명시된 오피스텔만 업무용으로 표시하고 위험 판정한다")
    void explicitlyBusinessOfficetelIsLabeledAndJudgedDanger() {
        BuildingData building = cleanApartment();
        building.setBuildingType("OFFICETEL");
        building.setBuildingUse("업무용 오피스텔");

        CheckResultDTO result = service.evaluate(
                        cleanRegistry(), building, cleanPrice(), 300_000_000L,
                        "서울특별시 강남구 테헤란로 152"
                ).getCheckResultDTOS().stream()
                .filter(item -> item.getCheckType() == CheckType.BUILDING_USE)
                .findFirst()
                .orElseThrow();

        assertEquals(RiskLevel.DANGER, result.getRiskLevel());
        assertEquals("업무용 오피스텔", result.getEvidence().get("buildingUse"));
        assertEquals("업무용 오피스텔", result.getEvidence().get("buildingUseRaw"));
    }

    @Test
    @DisplayName("건물 전체에 업무시설이 있어도 해당 호가 업무용으로 명시되지 않으면 오피스텔 주의로 남긴다")
    void ambiguousOfficetelInMixedUseBuildingStaysCaution() {
        BuildingData building = cleanApartment();
        building.setBuildingType("OFFICETEL");
        building.setBuildingUse("오피스텔, 기계실, 전기실, 통신실");
        building.setBuildingLevelNonResidentialUses("업무시설, 오피스텔및근린생활시설");

        CheckResultDTO result = service.evaluate(
                        cleanRegistry(), building, cleanPrice(), 300_000_000L,
                        "서울특별시 강남구 테헤란로 152"
                ).getCheckResultDTOS().stream()
                .filter(item -> item.getCheckType() == CheckType.BUILDING_USE)
                .findFirst()
                .orElseThrow();

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
        assertEquals("오피스텔(용도 확인 필요)", result.getEvidence().get("buildingUse"));
    }

    @Test
    @DisplayName("업무용 표기가 없는 일반 오피스텔은 주거용으로 간주한다")
    void plainOfficetelIsSafe() {
        BuildingData building = cleanApartment();
        building.setBuildingType("OFFICETEL");
        building.setBuildingUse("오피스텔");

        CheckResultDTO result = service.evaluate(
                        cleanRegistry(), building, cleanPrice(), 300_000_000L,
                        "서울특별시 강남구 테헤란로 152"
                ).getCheckResultDTOS().stream()
                .filter(item -> item.getCheckType() == CheckType.BUILDING_USE)
                .findFirst()
                .orElseThrow();

        assertEquals(RiskLevel.SAFE, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
        assertEquals("오피스텔", result.getEvidence().get("buildingUse"));
    }

    @Test
    @DisplayName("업무시설과 오피스텔이 함께 적힌 전유부는 용도 확인 필요로 표시한다")
    void ambiguousBusinessFacilityOfficetelNeedsUseConfirmation() {
        BuildingData building = cleanApartment();
        building.setBuildingType("OFFICETEL");
        building.setBuildingUse("업무시설, 오피스텔");

        CheckResultDTO result = service.evaluate(
                        cleanRegistry(), building, cleanPrice(), 300_000_000L,
                        "서울특별시 강남구 테헤란로 152"
                ).getCheckResultDTOS().stream()
                .filter(item -> item.getCheckType() == CheckType.BUILDING_USE)
                .findFirst()
                .orElseThrow();

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(DataStatus.UNVERIFIED, result.getDataStatus());
        assertEquals(
                "오피스텔(용도 확인 필요)",
                result.getEvidence().get("buildingUse")
        );
    }

    @Test
    @DisplayName("계약 대상 호 자체가 근린생활시설이면 위험으로 판정한다")
    void nonResidentialUnitIsDanger() {
        BuildingData building = cleanApartment();
        building.setBuildingType("MULTI_HOUSEHOLD");
        building.setBuildingUse("제2종근린생활시설");

        CheckResultDTO result = service.evaluate(
                        cleanRegistry(), building, cleanPrice(), 300_000_000L,
                        "서울특별시 강남구 테헤란로 152"
                ).getCheckResultDTOS().stream()
                .filter(item -> item.getCheckType() == CheckType.BUILDING_USE)
                .findFirst()
                .orElseThrow();

        assertEquals(RiskLevel.DANGER, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    @Test
    @DisplayName("HUG 보증금 한도 초과만으로 깡통전세를 확정하지 않는다")
    void hugDepositLimitAloneDoesNotMakeUnderwaterJeonseDanger() {
        RiskEvaluationResultDTO evaluation = service.evaluate(
                cleanRegistry(),
                cleanApartment(),
                cleanPrice(),
                // 수도권 HUG 한도 7억은 넘지만 전세가율은 71%에 그친다.
                1_000_000_000L,
                "서울특별시 강남구 테헤란로 152"
        );

        CheckResultDTO eligibility = evaluation.getCheckResultDTOS().stream()
                .filter(item -> item.getCheckType() == CheckType.HUG_GUARANTEE_ELIGIBILITY)
                .findFirst()
                .orElseThrow();
        DetailResultDTO precheck = evaluation.getFraudTypeResultDTOS().stream()
                .flatMap(type -> type.getDetails().stream())
                .filter(detail -> detail.getDetailType() == DetailType.HUG_GUARANTEE_PRECHECK)
                .findFirst()
                .orElseThrow();

        assertEquals(
                RiskLevel.DANGER,
                eligibility.getRiskLevel(),
                "필수점검 4번은 지역별 보증금 한도까지 본다"
        );
        assertEquals(
                RiskLevel.SAFE,
                precheck.getRiskLevel(),
                "깡통전세 1-C는 가격과 채무의 관계만 본다"
        );
    }

    @Test
    @DisplayName("실거래가가 없으면 공시가격을 140% 환산해 기준가로 쓴다")
    void officialPriceFallbackIsConverted() {
        PriceData officialOnly = new PriceData();
        officialOnly.setOfficialPrice(1_000_000_000L);

        CheckResultDTO result = service.evaluate(
                        cleanRegistry(), cleanApartment(), officialOnly, 300_000_000L,
                        "서울특별시 강남구 테헤란로 152"
                ).getCheckResultDTOS().stream()
                .filter(item -> item.getCheckType() == CheckType.HUG_GUARANTEE_ELIGIBILITY)
                .findFirst()
                .orElseThrow();

        assertEquals(1_400_000_000L, result.getEvidence().get("basePrice"));
        assertEquals(
                "OFFICIAL_PRICE_CONVERTED",
                result.getEvidence().get("basePriceSource")
        );
    }

    @Test
    @DisplayName("실거래가/공시가격/기준가 출처가 RiskEvaluationResultDTO 최상위에도 그대로 노출된다")
    void exposesPriceFieldsOnEvaluationResult() {
        PriceData recentSaleOnly = new PriceData();
        recentSaleOnly.setRecentSalePrice(900_000_000L);

        RiskEvaluationResultDTO withRecentSale = service.evaluate(
                cleanRegistry(), cleanApartment(), recentSaleOnly, 300_000_000L,
                "서울특별시 강남구 테헤란로 152"
        );

        assertEquals(900_000_000L, withRecentSale.getRecentSalePrice());
        assertEquals(null, withRecentSale.getOfficialPrice());
        assertEquals("RECENT_SALE_PRICE", withRecentSale.getBasePriceSource());

        PriceData officialOnly = new PriceData();
        officialOnly.setOfficialPrice(1_000_000_000L);

        RiskEvaluationResultDTO withOfficialOnly = service.evaluate(
                cleanRegistry(), cleanApartment(), officialOnly, 300_000_000L,
                "서울특별시 강남구 테헤란로 152"
        );

        // 최상위 필드는 환산 전 원본값을 그대로 담는다. 환산된 값(1.4배)은
        // basePrice(HUG evidence)에만 있고, officialPrice 자체는 원본을 유지해야
        // 프론트가 "공시가격: N원" 같은 원본 표시를 할 수 있다.
        assertEquals(1_000_000_000L, withOfficialOnly.getOfficialPrice());
        assertEquals(null, withOfficialOnly.getRecentSalePrice());
        assertEquals("OFFICIAL_PRICE_CONVERTED", withOfficialOnly.getBasePriceSource());
    }

    @Test
    @DisplayName("실거래가/공시가격이 둘 다 없으면 기준가 출처도 null이다")
    void exposesNullPriceFieldsWhenNoPriceDataAtAll() {
        RiskEvaluationResultDTO result = service.evaluate(
                cleanRegistry(), cleanApartment(), new PriceData(), 300_000_000L,
                "서울특별시 강남구 테헤란로 152"
        );

        assertEquals(null, result.getRecentSalePrice());
        assertEquals(null, result.getOfficialPrice());
        assertEquals(null, result.getBasePriceSource());
    }

    private RegistryData cleanRegistry() {
        RegistryData registry = new RegistryData();
        registry.setMortgageAmount(0L);
        registry.setHasSeizure(false);
        registry.setHasTrustRegistration(false);
        registry.setHasPostTrustInfringement(false);
        registry.setOwnerNames(List.of("홍길동"));
        registry.setOwnerName("홍길동");
        registry.setOwnerType("INDIVIDUAL");
        return registry;
    }

    private BuildingData cleanApartment() {
        BuildingData building = new BuildingData();
        building.setBuildingType("APARTMENT");
        building.setBuildingUse("아파트");
        building.setIsIllegalBuilding(false);
        building.setIllegalBuildingVerified(true);
        return building;
    }

    private PriceData cleanPrice() {
        PriceData price = new PriceData();
        price.setRecentSalePrice(1_400_000_000L);
        return price;
    }

    private DetailResultDTO ownershipMismatchOf(String buildingType) {
        BuildingData building = new BuildingData();
        building.setBuildingType(buildingType);
        building.setBuildingUse("공동주택");

        return service.evaluate(
                        new RegistryData(), building, price(1_000_000_000L),
                        100_000_000L, "서울특별시 강남구"
                ).getFraudTypeResultDTOS().stream()
                .filter(f -> f.getFraudType() == FraudType.FALSE_INFORMATION_RIGHTS_CONCEALMENT)
                .flatMap(f -> f.getDetails().stream())
                .filter(d -> d.getDetailType() == DetailType.LAND_BUILDING_OWNERSHIP_MISMATCH)
                .findFirst()
                .orElseThrow();
    }

    private PriceData price(Long recentSalePrice) {
        PriceData price = new PriceData();
        price.setRecentSalePrice(recentSalePrice);
        return price;
    }

    private CheckResultDTO evaluateMortgage(RegistryData registry, PriceData price) {
        BuildingData building = new BuildingData();
        building.setBuildingUse("공동주택");
        building.setBuildingType("APARTMENT");
        building.setIsIllegalBuilding(false);

        RiskEvaluationResultDTO evaluation = service.evaluate(
                registry, building, price, 100_000_000L, "서울특별시 강남구"
        );

        return evaluation.getCheckResultDTOS().stream()
                .filter(result -> result.getCheckType() == CheckType.MORTGAGE_EXISTENCE)
                .findFirst()
                .orElseThrow();
    }

    private CheckResultDTO evaluateIllegalBuilding(Boolean illegal) {
        BuildingData building = new BuildingData();
        building.setIsIllegalBuilding(illegal);
        building.setBuildingUse("공동주택");
        building.setBuildingType("APARTMENT");

        RiskEvaluationResultDTO evaluation = service.evaluate(
                null, building, null, 100_000_000L, "서울특별시 강남구"
        );

        return evaluation.getCheckResultDTOS().stream()
                .filter(result -> result.getCheckType() == CheckType.ILLEGAL_BUILDING)
                .findFirst()
                .orElseThrow();
    }
}
