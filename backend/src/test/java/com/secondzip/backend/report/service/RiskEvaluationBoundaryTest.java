package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.CheckResult;
import com.secondzip.backend.report.dto.DetailResult;
import com.secondzip.backend.report.dto.RiskEvaluationResult;
import com.secondzip.backend.report.dto.external.BuildingData;
import com.secondzip.backend.report.dto.external.PriceData;
import com.secondzip.backend.report.dto.external.RegistryData;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskEvaluationBoundaryTest {

    private final RiskEvaluationService service = new RiskEvaluationService();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "아파트, SAFE",
            "'주거용 오피스텔', SAFE",
            "'업무용 오피스텔', CAUTION",
            "근린생활시설, DANGER"
    })
    @DisplayName("건축물 용도 문자열을 주거·확인필요·비주거로 구분한다")
    void classifiesBuildingUse(String buildingUse, RiskLevel expected) {
        BuildingData building = baselineBuilding();
        building.setBuildingUse(buildingUse);

        CheckResult result = check(
                evaluate(baselineRegistry(), building, price(1_000_000_000L), 500_000_000L, "서울"),
                CheckType.BUILDING_USE
        );

        assertEquals(expected, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    @ParameterizedTest(name = "deposit={0} -> {1}")
    @MethodSource("jeonseRatioBoundaries")
    @DisplayName("전세가율 70%와 80% 경계값을 명세대로 판정한다")
    void appliesJeonseRatioBoundaries(long deposit, RiskLevel expected) {
        DetailResult result = detail(
                evaluate(
                        baselineRegistry(),
                        baselineBuilding(),
                        price(1_000_000_000L),
                        deposit,
                        "서울"
                ),
                DetailType.HIGH_JEONSE_RATIO
        );

        assertEquals(expected, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    static Stream<Arguments> jeonseRatioBoundaries() {
        return Stream.of(
                Arguments.of(699_999_999L, RiskLevel.SAFE),
                Arguments.of(700_000_000L, RiskLevel.CAUTION),
                Arguments.of(799_999_999L, RiskLevel.CAUTION),
                Arguments.of(800_000_000L, RiskLevel.DANGER)
        );
    }

    @ParameterizedTest(name = "mortgage={0} -> {1}")
    @CsvSource({
            "0, SAFE",
            "540000000, CAUTION",
            "540000001, DANGER"
    })
    @DisplayName("선순위채권 비율 54% 초과 여부를 경계값까지 구분한다")
    void appliesPriorityDebtBoundary(long mortgage, RiskLevel expected) {
        RegistryData registry = baselineRegistry();
        registry.setMortgageAmount(mortgage);

        DetailResult result = detail(
                evaluate(
                        registry,
                        baselineBuilding(),
                        price(1_000_000_000L),
                        100_000_000L,
                        "서울"
                ),
                DetailType.PRIORITY_DEBT_BURDEN
        );

        assertEquals(expected, result.getRiskLevel());
    }

    @ParameterizedTest(name = "{0}, deposit={1} -> {2}")
    @MethodSource("regionalDepositLimits")
    @DisplayName("HUG 사전점검은 수도권 7억·비수도권 5억 보증금 한도를 적용한다")
    void appliesRegionalDepositLimits(
            String roadAddress,
            long deposit,
            RiskLevel expected
    ) {
        CheckResult result = check(
                evaluate(
                        baselineRegistry(),
                        baselineBuilding(),
                        price(1_000_000_000L),
                        deposit,
                        roadAddress
                ),
                CheckType.HUG_GUARANTEE_ELIGIBILITY
        );

        assertEquals(expected, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    static Stream<Arguments> regionalDepositLimits() {
        return Stream.of(
                Arguments.of("서울특별시 강남구", 700_000_000L, RiskLevel.SAFE),
                Arguments.of("경기도 성남시", 700_000_001L, RiskLevel.DANGER),
                Arguments.of("부산광역시 해운대구", 500_000_000L, RiskLevel.SAFE),
                Arguments.of("부산광역시 해운대구", 500_000_001L, RiskLevel.DANGER)
        );
    }

    @ParameterizedTest(name = "{0} -> CAUTION")
    @CsvSource({"MULTI_FAMILY", "MULTI_HOUSEHOLD"})
    @DisplayName("다가구·다세대 계열은 HUG 실제 심사 필요 상태로 둔다")
    void marksComplexHousingAsCaution(String buildingType) {
        BuildingData building = baselineBuilding();
        building.setBuildingType(buildingType);

        CheckResult result = check(
                evaluate(baselineRegistry(), building, null, null, "서울"),
                CheckType.HUG_GUARANTEE_ELIGIBILITY
        );

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    @Test
    @DisplayName("주소를 확인하지 못하면 HUG 지역 판정은 확인 불가다")
    void cannotJudgeHugWithoutRegion() {
        CheckResult result = check(
                evaluate(
                        baselineRegistry(),
                        baselineBuilding(),
                        price(1_000_000_000L),
                        100_000_000L,
                        null
                ),
                CheckType.HUG_GUARANTEE_ELIGIBILITY
        );

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(DataStatus.UNVERIFIED, result.getDataStatus());
    }

    @ParameterizedTest(name = "postTrust={0} -> {1}/{2}")
    @MethodSource("postTrustCases")
    @DisplayName("신탁 이후 권리침해의 미확인·없음·있음을 서로 다르게 판정한다")
    void evaluatesPostTrustInfringement(
            Boolean postTrustInfringement,
            RiskLevel expectedRisk,
            DataStatus expectedStatus
    ) {
        RegistryData registry = baselineRegistry();
        registry.setHasTrustRegistration(true);
        registry.setHasPostTrustInfringement(postTrustInfringement);

        DetailResult result = detail(
                evaluate(
                        registry,
                        baselineBuilding(),
                        price(1_000_000_000L),
                        100_000_000L,
                        "서울"
                ),
                DetailType.POST_TRUST_RIGHTS_INFRINGEMENT
        );

        assertEquals(expectedRisk, result.getRiskLevel());
        assertEquals(expectedStatus, result.getDataStatus());
    }

    static Stream<Arguments> postTrustCases() {
        return Stream.of(
                Arguments.of(null, RiskLevel.CAUTION, DataStatus.UNVERIFIED),
                Arguments.of(false, RiskLevel.CAUTION, DataStatus.VERIFIED),
                Arguments.of(true, RiskLevel.DANGER, DataStatus.VERIFIED)
        );
    }

    @Test
    @DisplayName("신탁등기가 없으면 신탁 이후 권리침해 항목은 안전이다")
    void noTrustRegistrationIsSafe() {
        DetailResult result = detail(
                evaluate(
                        baselineRegistry(),
                        baselineBuilding(),
                        price(1_000_000_000L),
                        100_000_000L,
                        "서울"
                ),
                DetailType.POST_TRUST_RIGHTS_INFRINGEMENT
        );

        assertEquals(RiskLevel.SAFE, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    @ParameterizedTest(name = "{0}/{1} -> {2}")
    @MethodSource("ownerCases")
    @DisplayName("단독·다가구의 건물/토지 소유자 일치 여부를 판정한다")
    void comparesBuildingAndLandOwners(
            String buildingOwner,
            String landOwner,
            RiskLevel expected
    ) {
        RegistryData registry = baselineRegistry();
        registry.setOwnerName(buildingOwner);
        registry.setLandOwnerName(landOwner);
        BuildingData building = baselineBuilding();
        building.setBuildingType("SINGLE_FAMILY");

        DetailResult result = detail(
                evaluate(
                        registry,
                        building,
                        price(1_000_000_000L),
                        100_000_000L,
                        "서울"
                ),
                DetailType.LAND_BUILDING_OWNERSHIP_MISMATCH
        );

        assertEquals(expected, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    static Stream<Arguments> ownerCases() {
        return Stream.of(
                Arguments.of("홍길동", "홍길동", RiskLevel.SAFE),
                Arguments.of("홍길동", "김임대", RiskLevel.DANGER)
        );
    }

    private RiskEvaluationResult evaluate(
            RegistryData registry,
            BuildingData building,
            PriceData price,
            Long deposit,
            String roadAddress
    ) {
        return service.evaluate(registry, building, price, deposit, roadAddress);
    }

    private CheckResult check(RiskEvaluationResult evaluation, CheckType type) {
        return evaluation.getCheckResults().stream()
                .filter(result -> result.getCheckType() == type)
                .findFirst()
                .orElseThrow();
    }

    private DetailResult detail(
            RiskEvaluationResult evaluation,
            DetailType type
    ) {
        return evaluation.getFraudTypeResults().stream()
                .flatMap(result -> result.getDetails().stream())
                .filter(result -> result.getDetailType() == type)
                .findFirst()
                .orElseThrow();
    }

    private RegistryData baselineRegistry() {
        RegistryData registry = new RegistryData();
        registry.setMortgageAmount(0L);
        registry.setHasSeizure(false);
        registry.setHasTrustRegistration(false);
        registry.setHasPostTrustInfringement(false);
        registry.setOwnerName("홍길동");
        registry.setLandOwnerName("홍길동");
        registry.setOwnerType("INDIVIDUAL");
        return registry;
    }

    private BuildingData baselineBuilding() {
        BuildingData building = new BuildingData();
        building.setIsIllegalBuilding(false);
        building.setIllegalBuildingVerified(true);
        building.setBuildingUse("아파트");
        building.setBuildingType("APARTMENT");
        return building;
    }

    private PriceData price(long recentSalePrice) {
        PriceData price = new PriceData();
        price.setRecentSalePrice(recentSalePrice);
        return price;
    }
}
