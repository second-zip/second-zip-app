package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.CheckResult;
import com.secondzip.backend.report.dto.DetailResult;
import com.secondzip.backend.report.dto.FraudTypeResult;
import com.secondzip.backend.report.dto.RiskEvaluationResult;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RiskEvaluationServiceTest {

    private final RiskEvaluationService service = new RiskEvaluationService();

    @Test
    @DisplayName("위반건축물로 확인되면 위험으로 판정한다")
    void illegalBuildingIsDanger() {
        CheckResult result = evaluateIllegalBuilding(true);

        assertEquals(RiskLevel.DANGER, result.getRiskLevel());
        assertEquals(true, result.getEvidence().get("isIllegalBuildingVerified"));
    }

    @Test
    @DisplayName("위반건축물이 아닌 것으로 확인되면 안전으로 판정한다")
    void verifiedLegalBuildingIsSafe() {
        CheckResult result = evaluateIllegalBuilding(false);

        assertEquals(RiskLevel.SAFE, result.getRiskLevel());
        assertEquals(true, result.getEvidence().get("isIllegalBuildingVerified"));
    }

    @Test
    @DisplayName("위반건축물 정보를 제공받지 못하면 주의로 판정한다")
    void unknownIllegalBuildingStatusIsCaution() {
        CheckResult result = evaluateIllegalBuilding(null);

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(false, result.getEvidence().get("isIllegalBuildingVerified"));
        assertEquals(null, result.getEvidence().get("isIllegalBuilding"));
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

        RiskEvaluationResult evaluation = service.evaluate(
                null,
                building,
                null,
                100_000_000L,
                "서울특별시 강남구"
        );
        CheckResult result = evaluation.getCheckResults().stream()
                .filter(item ->
                        item.getCheckType() == CheckType.ILLEGAL_BUILDING
                )
                .findFirst()
                .orElseThrow();

        assertEquals(
                "CODEF_BUILDING_REGISTER",
                result.getEvidence().get("source")
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

        CheckResult result = evaluateMortgage(registry, price(1_000_000_000L));

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

        CheckResult result = evaluateMortgage(registry, price(1_000_000_000L));

        assertEquals(RiskLevel.SAFE, result.getRiskLevel());
        assertEquals(0L, result.getEvidence().get("mortgageAmount"));
    }

    @Test
    @DisplayName("기준가가 0이면 비율을 계산하지 않고 주의로 판정한다")
    void zeroBasePriceDoesNotProduceInfiniteRatio() {
        RegistryData registry = new RegistryData();
        registry.setMortgageAmount(100_000_000L);

        CheckResult result = evaluateMortgage(registry, price(0L));

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
    }

    @Test
    @DisplayName("확인 불가는 위험도가 아니라 데이터 상태로 구분된다")
    void unverifiedIsDistinguishableFromRealRisk() {
        CheckResult unknown = evaluateMortgage(new RegistryData(), price(1_000_000_000L));

        assertEquals(RiskLevel.CAUTION, unknown.getRiskLevel());
        assertEquals(DataStatus.UNVERIFIED, unknown.getDataStatus());

        RegistryData withMortgage = new RegistryData();
        withMortgage.setMortgageAmount(100_000_000L);
        CheckResult real = evaluateMortgage(withMortgage, price(1_000_000_000L));

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
        DetailResult ownership = ownershipMismatchOf("APARTMENT");

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
        DetailResult ownership = ownershipMismatchOf("SINGLE_FAMILY");

        assertEquals(DataStatus.UNVERIFIED, ownership.getDataStatus());
        assertEquals(RiskLevel.CAUTION, ownership.getRiskLevel());
    }

    @Test
    @DisplayName("해당 없는 항목은 유형 집계에서 제외된다")
    void notApplicableDetailIsExcludedFromAggregation() {
        // 아파트 + 등기 정보 없음 → 유형2의 3개 중 1개는 NOT_APPLICABLE,
        // 나머지 2개는 CAUTION(용도 확인 불가, 권리침해 확인 불가)
        BuildingData building = new BuildingData();
        building.setBuildingType("APARTMENT");

        FraudTypeResult type2 = service.evaluate(
                        null, building, price(1_000_000_000L), 100_000_000L, "서울특별시 강남구"
                ).getFraudTypeResults().stream()
                .filter(f -> f.getFraudType() == FraudType.FALSE_INFORMATION_RIGHTS_CONCEALMENT)
                .findFirst()
                .orElseThrow();

        assertEquals(
                RiskLevel.DANGER,
                type2.getRiskLevel(),
                "해당되는 2개가 모두 CAUTION이면 DANGER. "
                        + "임계값을 3으로 고정하면 아파트는 이 조건에 영원히 도달하지 못한다."
        );
    }

    private DetailResult ownershipMismatchOf(String buildingType) {
        BuildingData building = new BuildingData();
        building.setBuildingType(buildingType);
        building.setBuildingUse("공동주택");

        return service.evaluate(
                        new RegistryData(), building, price(1_000_000_000L),
                        100_000_000L, "서울특별시 강남구"
                ).getFraudTypeResults().stream()
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

    private CheckResult evaluateMortgage(RegistryData registry, PriceData price) {
        BuildingData building = new BuildingData();
        building.setBuildingUse("공동주택");
        building.setBuildingType("APARTMENT");
        building.setIsIllegalBuilding(false);

        RiskEvaluationResult evaluation = service.evaluate(
                registry, building, price, 100_000_000L, "서울특별시 강남구"
        );

        return evaluation.getCheckResults().stream()
                .filter(result -> result.getCheckType() == CheckType.MORTGAGE_EXISTENCE)
                .findFirst()
                .orElseThrow();
    }

    private CheckResult evaluateIllegalBuilding(Boolean illegal) {
        BuildingData building = new BuildingData();
        building.setIsIllegalBuilding(illegal);
        building.setBuildingUse("공동주택");
        building.setBuildingType("APARTMENT");

        RiskEvaluationResult evaluation = service.evaluate(
                null, building, null, 100_000_000L, "서울특별시 강남구"
        );

        return evaluation.getCheckResults().stream()
                .filter(result -> result.getCheckType() == CheckType.ILLEGAL_BUILDING)
                .findFirst()
                .orElseThrow();
    }
}
