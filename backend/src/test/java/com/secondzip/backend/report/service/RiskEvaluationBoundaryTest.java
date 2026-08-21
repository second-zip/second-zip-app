package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.CheckResultDTO;
import com.secondzip.backend.report.dto.DetailResultDTO;
import com.secondzip.backend.report.dto.RiskEvaluationResultDTO;
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
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskEvaluationBoundaryTest {

    private final RiskEvaluationService service = new RiskEvaluationService();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "아파트, SAFE",
            "'주거용 오피스텔', SAFE",
            "'업무시설, 주거용 오피스텔', SAFE",
            "'업무용 오피스텔', DANGER",
            "'비주거용 오피스텔', DANGER",
            "'제2종근린생활시설, 다가구주택', DANGER",
            "근린생활시설, DANGER"
    })
    @DisplayName("건축물 용도 문자열을 주거·확인필요·비주거로 구분한다")
    void classifiesBuildingUse(String buildingUse, RiskLevel expected) {
        BuildingData building = baselineBuilding();
        building.setBuildingUse(buildingUse);

        CheckResultDTO result = check(
                evaluate(baselineRegistry(), building, price(1_000_000_000L), 500_000_000L, "서울"),
                CheckType.BUILDING_USE
        );

        assertEquals(expected, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"계단실", "업무시설, 오피스텔"})
    @DisplayName("주거 여부를 파싱할 수 없는 부속용도는 위험 확정이 아니라 미확인이다")
    void unknownBuildingUseIsUnverified(String buildingUse) {
        BuildingData building = baselineBuilding();
        building.setBuildingUse(buildingUse);

        CheckResultDTO result = check(
                evaluate(baselineRegistry(), building, price(1_000_000_000L),
                        500_000_000L, "서울특별시 강남구 테헤란로 11"),
                CheckType.BUILDING_USE
        );

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(DataStatus.UNVERIFIED, result.getDataStatus());
    }

    @ParameterizedTest(name = "아파트 + {0} -> DANGER")
    @ValueSource(strings = {
            "문화및집회시설", "운수시설", "운동시설", "수련시설",
            "위험물저장및처리시설", "동물및식물관련시설", "자원순환관련시설",
            "교정시설", "국방·군사시설", "방송통신시설", "발전시설",
            "묘지관련시설", "관광휴게시설", "장례시설", "야영장시설"
    })
    @DisplayName("건축법상 비주거 용도가 주거 용도와 섞이면 비주거를 우선한다")
    void nonResidentialUseWinsInMixedPurpose(String nonResidentialUse) {
        BuildingData building = baselineBuilding();
        building.setBuildingUse("아파트, " + nonResidentialUse);

        CheckResultDTO result = check(
                evaluate(baselineRegistry(), building, price(1_000_000_000L),
                        500_000_000L, "서울특별시 강남구 테헤란로 11"),
                CheckType.BUILDING_USE
        );

        assertEquals(RiskLevel.DANGER, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    @ParameterizedTest(name = "deposit={0} -> {1}")
    @MethodSource("jeonseRatioBoundaries")
    @DisplayName("전세가율 70%와 80% 경계값을 명세대로 판정한다")
    void appliesJeonseRatioBoundaries(long deposit, RiskLevel expected) {
        DetailResultDTO result = detail(
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

        DetailResultDTO result = detail(
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
        CheckResultDTO result = check(
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
                Arguments.of("서울특별시 강남구 테헤란로 11", 700_000_000L, RiskLevel.SAFE),
                Arguments.of("경기도 성남시 분당구 판교로 1", 700_000_001L, RiskLevel.DANGER),
                Arguments.of("부산광역시 해운대구 해운대로 1", 500_000_000L, RiskLevel.SAFE),
                Arguments.of("부산광역시 해운대구 해운대로 1", 500_000_001L, RiskLevel.DANGER)
        );
    }

    @Test
    @DisplayName("다가구는 선순위 임차보증금이 없어서 HUG 통과로 확정하지 않는다")
    void multiFamilyNeedsSeniorTenantDeposits() {
        BuildingData building = baselineBuilding();
        building.setBuildingType("MULTI_FAMILY");
        building.setBuildingUse("다가구주택");

        CheckResultDTO result = check(
                evaluate(baselineRegistry(), building, price(1_000_000_000L),
                        100_000_000L, "서울특별시 강남구 테헤란로 11"),
                CheckType.HUG_GUARANTEE_ELIGIBILITY
        );

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(DataStatus.UNVERIFIED, result.getDataStatus());
    }

    @Test
    @DisplayName("다세대는 고정 CAUTION으로 조기 반환하지 않고 HUG 조건을 계산한다")
    void multiHouseholdIsCalculated() {
        BuildingData building = baselineBuilding();
        building.setBuildingType("MULTI_HOUSEHOLD");
        building.setBuildingUse("다세대주택");

        CheckResultDTO result = check(
                evaluate(baselineRegistry(), building, price(1_000_000_000L),
                        100_000_000L, "서울특별시 강남구 테헤란로 11"),
                CheckType.HUG_GUARANTEE_ELIGIBILITY
        );

        assertEquals(RiskLevel.SAFE, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    @Test
    @DisplayName("주소를 확인하지 못하면 HUG 지역 판정은 확인 불가다")
    void cannotJudgeHugWithoutRegion() {
        CheckResultDTO result = check(
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

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "서울특별시 강남구 역삼동 737",
            "서울특별시 종로구 종로1가 1-1",
            "강남구 테헤란로 11",
            "광주시 경안로 11",
            "대한민국 서울특별시 강남구 테헤란로 11"
    })
    @DisplayName("시·도를 확인할 수 없는 주소는 비수도권으로 추측하지 않는다")
    void blankOrLotAddressIsUnknown(String roadAddress) {
        CheckResultDTO result = check(
                evaluate(baselineRegistry(), baselineBuilding(), price(1_000_000_000L),
                        100_000_000L, roadAddress),
                CheckType.HUG_GUARANTEE_ELIGIBILITY
        );

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(DataStatus.UNVERIFIED, result.getDataStatus());
    }

    @Test
    @DisplayName("HUG 가격은 공시가격 140%를 최근 실거래가보다 우선 적용한다")
    void hugUsesConvertedOfficialPriceBeforeRecentSale() {
        PriceData price = new PriceData();
        price.setRecentSalePrice(1_000_000_000L);
        price.setOfficialPrice(500_000_000L);

        CheckResultDTO result = check(
                evaluate(baselineRegistry(), baselineBuilding(), price,
                        650_000_000L, "서울특별시 강남구 테헤란로 11"),
                CheckType.HUG_GUARANTEE_ELIGIBILITY
        );

        // 공시가격 5억 × 140% × 담보인정비율 90% = 6.3억
        assertEquals(RiskLevel.DANGER, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    @Test
    @DisplayName("실거래가 0은 공시가격 폴백을 막지 않고, 폴백 기준가는 140% 환산액이다")
    void zeroRecentSaleFallsBackToConvertedOfficialPrice() {
        PriceData price = new PriceData();
        price.setRecentSalePrice(0L);
        price.setOfficialPrice(1_000_000_000L);

        // 공시가 10억 → 기준가 14억. 보증금 10.5억이면 전세가율 75%.
        DetailResultDTO result = detail(
                evaluate(baselineRegistry(), baselineBuilding(), price,
                        1_050_000_000L, "서울특별시 강남구 테헤란로 11"),
                DetailType.HIGH_JEONSE_RATIO
        );

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    @Test
    @DisplayName("공시가격을 환산하지 않으면 정상 매물이 전세가율 위험으로 뒤집힌다")
    void officialPriceFallbackDoesNotInflateJeonseRatio() {
        PriceData price = new PriceData();
        price.setOfficialPrice(1_000_000_000L);

        // 환산 전 기준가 10억이면 75%(CAUTION), 환산 후 14억이면 53.6%(SAFE).
        DetailResultDTO result = detail(
                evaluate(baselineRegistry(), baselineBuilding(), price,
                        750_000_000L, "서울특별시 강남구 테헤란로 11"),
                DetailType.HIGH_JEONSE_RATIO
        );

        assertEquals(RiskLevel.SAFE, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    @Test
    @DisplayName("비주거 오피스텔은 금액이 맞아도 HUG 가입 가능으로 판정하지 않는다")
    void nonResidentialOfficetelFailsHugEligibility() {
        BuildingData building = baselineBuilding();
        building.setBuildingType("OFFICETEL");
        building.setBuildingUse("업무용 오피스텔");

        CheckResultDTO result = check(
                evaluate(baselineRegistry(), building, price(1_000_000_000L),
                        100_000_000L, "서울특별시 강남구 테헤란로 11"),
                CheckType.HUG_GUARANTEE_ELIGIBILITY
        );

        assertEquals(RiskLevel.DANGER, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    @ParameterizedTest
    @CsvSource({"true, false", "false, true"})
    @DisplayName("위반건축물 또는 권리침해가 확인되면 HUG 위험이다")
    void violationOrRightsInfringementFailsHug(boolean illegal, boolean seizure) {
        BuildingData building = baselineBuilding();
        building.setIsIllegalBuilding(illegal);
        RegistryData registry = baselineRegistry();
        registry.setHasSeizure(seizure);

        CheckResultDTO result = check(
                evaluate(registry, building, price(1_000_000_000L),
                        100_000_000L, "서울특별시 강남구 테헤란로 11"),
                CheckType.HUG_GUARANTEE_ELIGIBILITY
        );

        assertEquals(RiskLevel.DANGER, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    @Test
    @DisplayName("등기 응답에 소유자 이름이 빠진 부분 응답은 HUG 통과로 확정하지 않는다")
    void unknownRegistryOwnerMakesHugUnverified() {
        RegistryData registry = baselineRegistry();
        registry.setOwnerName(null);
        registry.setOwnerNames(null);

        CheckResultDTO result = check(
                evaluate(registry, baselineBuilding(), price(1_000_000_000L),
                        100_000_000L, "서울특별시 강남구 테헤란로 11"),
                CheckType.HUG_GUARANTEE_ELIGIBILITY
        );

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(DataStatus.UNVERIFIED, result.getDataStatus());
    }

    @Test
    @DisplayName("소유자 이름은 있어도 유형이 누락된 등기 응답은 HUG 통과로 확정하지 않는다")
    void unknownRegistryOwnerTypeMakesHugUnverified() {
        RegistryData registry = baselineRegistry();
        registry.setOwnerType(null);

        CheckResultDTO result = check(
                evaluate(registry, baselineBuilding(), price(1_000_000_000L),
                        100_000_000L, "서울특별시 강남구 테헤란로 11"),
                CheckType.HUG_GUARANTEE_ELIGIBILITY
        );

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(DataStatus.UNVERIFIED, result.getDataStatus());
    }

    @Test
    @DisplayName("신탁회사 소유인데 신탁등기가 없다는 모순 응답은 HUG 가능으로 확정하지 않는다")
    void trustCompanyOwnerCannotBeVerifiedSafeForHug() {
        RegistryData registry = baselineRegistry();
        registry.setOwnerType("TRUST_COMPANY");
        registry.setHasTrustRegistration(false);

        CheckResultDTO result = check(
                evaluate(registry, baselineBuilding(), price(1_000_000_000L),
                        100_000_000L, "서울특별시 강남구 테헤란로 11"),
                CheckType.HUG_GUARANTEE_ELIGIBILITY
        );

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(DataStatus.UNVERIFIED, result.getDataStatus());
    }

    @Test
    @DisplayName("단독주택의 건물·토지 공동소유자 집합이 다르면 HUG 위험이다")
    void ownerSetMismatchFailsHugEligibility() {
        RegistryData registry = baselineRegistry();
        registry.setOwnerNames(java.util.List.of("홍길동", "김임대"));
        registry.setLandOwnerNames(java.util.List.of("홍길동"));
        BuildingData building = baselineBuilding();
        building.setBuildingType("SINGLE_FAMILY");
        building.setBuildingUse("단독주택");

        CheckResultDTO result = check(
                evaluate(registry, building, price(1_000_000_000L),
                        100_000_000L, "서울특별시 강남구 테헤란로 11"),
                CheckType.HUG_GUARANTEE_ELIGIBILITY
        );

        assertEquals(RiskLevel.DANGER, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    @Test
    @DisplayName("건물·토지 공동소유자는 순서·공백이 아닌 정규화 집합으로 비교한다")
    void comparesNormalizedOwnerSets() {
        RegistryData registry = baselineRegistry();
        registry.setOwnerNames(java.util.List.of("홍 길동", " 김임대 "));
        registry.setLandOwnerNames(java.util.List.of("김임대", "홍길동"));
        BuildingData building = baselineBuilding();
        building.setBuildingType("SINGLE_FAMILY");
        building.setBuildingUse("단독주택");

        DetailResultDTO result = detail(
                evaluate(registry, building, price(1_000_000_000L),
                        100_000_000L, "서울특별시 강남구 테헤란로 11"),
                DetailType.LAND_BUILDING_OWNERSHIP_MISMATCH
        );

        assertEquals(RiskLevel.SAFE, result.getRiskLevel());
        assertEquals(DataStatus.VERIFIED, result.getDataStatus());
    }

    @Test
    @DisplayName("구형 scalar 소유자 필드가 양쪽 모두 공백이어도 일치 SAFE로 보지 않는다")
    void blankScalarOwnersRemainUnverified() {
        RegistryData registry = baselineRegistry();
        registry.setOwnerName("   ");
        registry.setLandOwnerName("\t");
        BuildingData building = baselineBuilding();
        building.setBuildingType("SINGLE_FAMILY");
        building.setBuildingUse("단독주택");

        DetailResultDTO result = detail(
                evaluate(registry, building, price(1_000_000_000L),
                        100_000_000L, "서울특별시 강남구 테헤란로 11"),
                DetailType.LAND_BUILDING_OWNERSHIP_MISMATCH
        );

        assertEquals(RiskLevel.CAUTION, result.getRiskLevel());
        assertEquals(DataStatus.UNVERIFIED, result.getDataStatus());
    }

    @Test
    @DisplayName("신탁등기 없음과 신탁 이후 권리침해 있음이 같이 오면 SAFE로 확정하지 않는다")
    void contradictoryPostTrustFlagsRemainUnverified() {
        RegistryData registry = baselineRegistry();
        registry.setHasTrustRegistration(false);
        registry.setHasPostTrustInfringement(true);

        DetailResultDTO result = detail(
                evaluate(registry, baselineBuilding(), price(1_000_000_000L),
                        100_000_000L, "서울특별시 강남구 테헤란로 11"),
                DetailType.POST_TRUST_RIGHTS_INFRINGEMENT
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

        DetailResultDTO result = detail(
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
        DetailResultDTO result = detail(
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

        DetailResultDTO result = detail(
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

    private RiskEvaluationResultDTO evaluate(
            RegistryData registry,
            BuildingData building,
            PriceData price,
            Long deposit,
            String roadAddress
    ) {
        return service.evaluate(registry, building, price, deposit, roadAddress);
    }

    private CheckResultDTO check(RiskEvaluationResultDTO evaluation, CheckType type) {
        return evaluation.getCheckResultDTOS().stream()
                .filter(result -> result.getCheckType() == type)
                .findFirst()
                .orElseThrow();
    }

    private DetailResultDTO detail(
            RiskEvaluationResultDTO evaluation,
            DetailType type
    ) {
        return evaluation.getFraudTypeResultDTOS().stream()
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
