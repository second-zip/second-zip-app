package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.external.BuildingRegisterAnalysisData;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingRegisterDataParserTest {

    private final BuildingRegisterDataParser parser =
            new BuildingRegisterDataParser();

    @Test
    void violationInEitherCollectiveDocumentIsDangerEvidence() {
        Map<BuildingRegisterDocumentType, Map<String, Object>> documents = Map.of(
                BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                Map.of("resViolationStatus", ""),
                BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE,
                Map.of(
                        "resViolationStatus", "위반건축물",
                        "resOwnedList", List.of(Map.of("resUseType", "공동주택")),
                        "resPriceList", List.of(Map.of(
                                "resBasePrice",
                                "350,000,000원"
                        ))
                )
        );

        BuildingRegisterAnalysisData result = parser.parse(
                List.of(
                        BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                        BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE
                ),
                documents,
                "APARTMENT",
                "공동주택"
        );

        assertTrue(result.getBuildingData().getIsIllegalBuilding());
        assertTrue(result.getBuildingData().getIllegalBuildingVerified());
        assertEquals(
                "CODEF_BUILDING_REGISTER",
                result.getBuildingData().getIllegalBuildingSource()
        );
        assertFalse(
                result.getViolationByDocument().get("COLLECTIVE_TITLE")
        );
        assertTrue(
                result.getViolationByDocument().get("COLLECTIVE_EXCLUSIVE")
        );
        assertEquals(350_000_000L, result.getOfficialPrice());
        assertEquals("공동주택", result.getBuildingData().getBuildingUse());
    }

    @Test
    void picksMostRecentBasePriceNotTheLargest() {
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE),
                Map.of(
                        BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE,
                        Map.of(
                                "resViolationStatus", "",
                                "resPriceList", List.of(
                                        Map.of(
                                                "resBaseDate", "2023-01-01",
                                                "resBasePrice", "500,000,000원"
                                        ),
                                        Map.of(
                                                "resBaseDate", "2024-01-01",
                                                "resBasePrice", "420,000,000원"
                                        )
                                )
                        )
                ),
                "APARTMENT",
                "공동주택"
        );

        assertEquals(
                420_000_000L,
                result.getOfficialPrice(),
                "공시가격이 하락한 해가 있으면 최대값이 아니라 최신값을 써야 한다. "
                        + "기준가가 높으면 전세가율이 낮게 나와 위험을 과소평가한다."
        );
    }

    @Test
    void picksMostRecentBasePriceWhenDatedByResReferenceDate() {
        // 실제 응답에서 확인된 사례: 공시가격 이력의 날짜 필드가 DATE_KEYS에
        // 없던 resReferenceDate였다. 그러면 모든 항목이 "날짜 없음"으로
        // 처리되고, 금액이 해마다 다르니 최신값을 증명 못 해 매번 null이었다.
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE),
                Map.of(
                        BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE,
                        Map.of(
                                "resViolationStatus", "",
                                "resPriceList", List.of(
                                        Map.of(
                                                "resReferenceDate", "20260101",
                                                "resBasePrice", "111000000"
                                        ),
                                        Map.of(
                                                "resReferenceDate", "20250101",
                                                "resBasePrice", "107000000"
                                        ),
                                        Map.of(
                                                "resReferenceDate", "20240101",
                                                "resBasePrice", "105000000"
                                        ),
                                        Map.of(
                                                "resReferenceDate", "20230101",
                                                "resBasePrice", "96100000"
                                        )
                                )
                        )
                ),
                "MULTI_HOUSEHOLD",
                "공동주택"
        );

        assertEquals(111_000_000L, result.getOfficialPrice());
    }

    @Test
    void differentUndatedPricesRemainUnverified() {
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE),
                Map.of(
                        BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE,
                        Map.of(
                                "resViolationStatus", "",
                                "resPriceList", List.of(
                                        Map.of("resBasePrice", "300,000,000원"),
                                        Map.of("resBasePrice", "450,000,000원")
                                )
                        )
                ),
                "APARTMENT",
                "공동주택"
        );

        assertNull(result.getOfficialPrice());
    }

    @Test
    void unreadableNewerOfficialPriceDoesNotFallBackToOlderPrice() {
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE),
                Map.of(
                        BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE,
                        Map.of(
                                "resViolationStatus", "",
                                "resPriceList", List.of(
                                        Map.of(
                                                "resBaseDate", "2024-01-01",
                                                "resBasePrice", "420,000,000원"
                                        ),
                                        Map.of(
                                                "resBaseDate", "2025-01-01",
                                                "resBasePrice", "확인불가"
                                        )
                                )
                        )
                ),
                "APARTMENT",
                "공동주택"
        );

        assertNull(result.getOfficialPrice());
    }

    @Test
    void invalidCalendarDateCannotWinAsTheLatestPrice() {
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE),
                Map.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE, Map.of(
                        "resViolationStatus", "",
                        "resPriceList", List.of(
                                Map.of(
                                        "resBaseDate", "2023-02-28",
                                        "resBasePrice", "400,000,000"
                                ),
                                Map.of(
                                        "resBaseDate", "2023-02-31",
                                        "resBasePrice", "900,000,000"
                                )
                        )
                )),
                "APARTMENT",
                "공동주택"
        );

        assertNull(result.getOfficialPrice());
    }

    @Test
    void bindsUsePriceAreaAndFloorToTheRequestedDongAndHo() {
        Map<String, Object> exclusive = Map.of(
                "units", List.of(
                        Map.of(
                                "resDong", "101",
                                "children", List.of(
                                        Map.of(
                                                "resHo", "1202호",
                                                "resViolationStatus", "",
                                                "resUseType", "계단실",
                                                "resExclusiveArea", "12.00",
                                                "resFloor", "지하 1층"
                                        ),
                                        Map.of(
                                                "resHo", "1203",
                                                "resViolationStatus", "",
                                                "resUseType", "제2종근린생활시설",
                                                "resType1", "다가구주택",
                                                "resExclusiveArea", "84.25㎡",
                                                "resFloorNo", "3층"
                                        )
                                )
                        ),
                        Map.of(
                                "resDong", "102동",
                                "resHo", "1203호",
                                "resViolationStatus", "",
                                "resUseType", "아파트",
                                "resExclusiveArea", "99.00",
                                "resFloor", "12층"
                        )
                )
        );
        Map<String, Object> title = Map.of(
                "resViolationStatus", "",
                "units", List.of(
                        Map.of(
                                "resDong", "101동",
                                "rows", List.of(
                                        Map.of(
                                                "resHo", "1202호",
                                                "resBaseDate", "2024-01-01",
                                                "resBasePrice", "100,000,000"
                                        ),
                                        Map.of(
                                                "resHo", "1203호",
                                                "resBaseDate", "2024-01-01",
                                                "resBasePrice", "420,000,000"
                                        )
                                )
                        )
                )
        );

        BuildingRegisterAnalysisData result = parser.parse(
                List.of(
                        BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                        BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE
                ),
                Map.of(
                        BuildingRegisterDocumentType.COLLECTIVE_TITLE, title,
                        BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE, exclusive
                ),
                "MULTI_HOUSEHOLD",
                "공동주택, 다세대주택",
                "101동 1203호"
        );

        assertEquals(
                "제2종근린생활시설, 다가구주택",
                result.getBuildingData().getBuildingUse()
        );
        assertEquals(420_000_000L, result.getOfficialPrice());
        assertEquals(new BigDecimal("84.25"), result.getTransactionAreaSqm());
        assertEquals(3, result.getTransactionFloor());
    }

    @Test
    void refusesDataWhenOnlyTheHoMatchesButTheDongDoesNot() {
        Map<String, Object> wrongUnit = Map.of(
                "resDong", "102동",
                "resHo", "1203호",
                "resViolationStatus", "",
                "resUseType", "아파트",
                "resBaseDate", "2024-01-01",
                "resBasePrice", "500,000,000",
                "resExclusiveArea", "84.00",
                "resFloor", "12층"
        );

        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE),
                Map.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE, wrongUnit),
                "APARTMENT",
                "공동주택, 아파트",
                "101동 1203호"
        );

        assertNull(result.getBuildingData().getBuildingUse());
        assertNull(result.getOfficialPrice());
        assertNull(result.getTransactionAreaSqm());
        assertNull(result.getTransactionFloor());
        assertFalse(result.getBuildingData().getIllegalBuildingVerified());
    }

    @Test
    void sameHoAcrossDifferentDongsIsAmbiguousWhenDongWasNotProvided() {
        Map<String, Object> document = Map.of("units", List.of(
                Map.of(
                        "resDong", "101동",
                        "resHo", "1203호",
                        "resViolationStatus", "",
                        "resUseType", "아파트",
                        "resBasePrice", "400,000,000",
                        "resExclusiveArea", "84",
                        "resFloor", "3층"
                ),
                Map.of(
                        "resDong", "102동",
                        "resHo", "1203호",
                        "resViolationStatus", "",
                        "resUseType", "제2종근린생활시설",
                        "resBasePrice", "900,000,000",
                        "resExclusiveArea", "99",
                        "resFloor", "12층"
                )
        ));

        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE),
                Map.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE, document),
                "APARTMENT",
                "공동주택, 아파트",
                "1203호"
        );

        assertNull(result.getOfficialPrice());
        assertNull(result.getTransactionAreaSqm());
        assertNull(result.getTransactionFloor());
        assertFalse(result.getBuildingData().getIllegalBuildingVerified());
    }

    @Test
    void usesGeneralRegisterTotalAreaBeforeHubFallback() {
        BuildingRegisterAnalysisData codefArea = parser.parse(
                List.of(BuildingRegisterDocumentType.GENERAL),
                Map.of(BuildingRegisterDocumentType.GENERAL, Map.of(
                        "resViolationStatus", "",
                        "resUseType", "다가구주택",
                        "resTotalFloorArea", "201.25㎡"
                )),
                "MULTI_FAMILY",
                "단독주택, 다가구주택",
                null,
                new BigDecimal("999.00")
        );
        BuildingRegisterAnalysisData hubFallback = parser.parse(
                List.of(BuildingRegisterDocumentType.GENERAL),
                Map.of(BuildingRegisterDocumentType.GENERAL, Map.of(
                        "resViolationStatus", "",
                        "resUseType", "다가구주택"
                )),
                "MULTI_FAMILY",
                "단독주택, 다가구주택",
                null,
                new BigDecimal("199.50")
        );

        assertEquals(new BigDecimal("201.25"), codefArea.getTransactionAreaSqm());
        assertEquals(new BigDecimal("199.50"), hubFallback.getTransactionAreaSqm());
        assertNull(hubFallback.getTransactionFloor());
    }

    @Test
    void conflictingRegisterAreasDoNotFallBackToAnArbitraryArea() {
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.GENERAL),
                Map.of(BuildingRegisterDocumentType.GENERAL, Map.of(
                        "resViolationStatus", "",
                        "resUseType", "다가구주택",
                        "rows", List.of(
                                Map.of("resTotalFloorArea", "180.00"),
                                Map.of("resTotalFloorArea", "240.00")
                        )
                )),
                "MULTI_FAMILY",
                "단독주택, 다가구주택",
                null,
                new BigDecimal("240.00")
        );

        assertNull(result.getTransactionAreaSqm());
    }

    @Test
    void doesNotInferFloorFromTheHoNumber() {
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE),
                Map.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE, Map.of(
                        "resDong", "101동",
                        "resHo", "1203호",
                        "resViolationStatus", "",
                        "resUseType", "아파트",
                        "resExclusiveArea", "84"
                )),
                "APARTMENT",
                "공동주택, 아파트",
                "101동 1203호"
        );

        assertNull(result.getTransactionFloor());
    }

    @Test
    void unmarkedResAreaFallbackIsAcceptedWhenAllCandidatesAgree() {
        // CODEF 응답이 resExclusiveArea 계열 키 없이 resArea만 쓰고, 같은 값이
        // 요약행·상세행처럼 두 번 노출되며 전유/동호 표식이 전혀 없는 실제 사례.
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE),
                Map.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE, Map.of(
                        "resViolationStatus", "",
                        "resUseType", "업무시설",
                        "resArea", "20.24",
                        "detail", Map.of("resArea", "20.24")
                )),
                "OFFICETEL",
                "업무시설"
        );

        assertEquals(new BigDecimal("20.24"), result.getTransactionAreaSqm());
    }

    @Test
    void conflictingUnmarkedResAreaFallbackCandidatesRemainUnresolved() {
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE),
                Map.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE, Map.of(
                        "resViolationStatus", "",
                        "resUseType", "업무시설",
                        "resArea", "20.24",
                        "detail", Map.of("resArea", "45.10")
                )),
                "OFFICETEL",
                "업무시설"
        );

        assertNull(result.getTransactionAreaSqm());
    }

    @Test
    void resTypeZeroPicksTheUnitsOwnAreaAndFloorOutOfTheFullFloorOverview() {
        // 동/호 식별자가 전혀 없는 실제 오피스텔 응답. 대상 호실 자기 행은
        // resType=0이고, 그 호실에 딸린 공용부분 배분 행(전기실·계단실·
        // 주차장 등)은 다른 층 번호와 함께 resType=1로 섞여 내려온다.
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE),
                Map.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE, Map.of(
                        "resViolationStatus", "",
                        "rows", List.of(
                                Map.of(
                                        "resType", "0",
                                        "resFloor", "16층",
                                        "resUseType", "업무시설(오피스텔)",
                                        "resArea", "20.24"
                                ),
                                Map.of(
                                        "resType", "1",
                                        "resFloor", "지2층",
                                        "resUseType", "펌프실,전기실,발전기실,저수조",
                                        "resArea", "1.73"
                                ),
                                Map.of(
                                        "resType", "1",
                                        "resFloor", "지1층",
                                        "resUseType", "통신실,관리실",
                                        "resArea", "0.27"
                                ),
                                Map.of(
                                        "resType", "1",
                                        "resFloor", "각층",
                                        "resUseType", "계단실,복도,홀",
                                        "resArea", "8.15"
                                ),
                                Map.of(
                                        "resType", "1",
                                        "resFloor", "1층",
                                        "resUseType", "주차장",
                                        "resArea", "0.32"
                                )
                        )
                )),
                "OFFICETEL",
                "업무시설(오피스텔)"
        );

        assertEquals(new BigDecimal("20.24"), result.getTransactionAreaSqm());
        assertEquals(16, result.getTransactionFloor());
    }

    @Test
    void conflictingFloorsWithinTheTargetScopeRemainUnverified() {
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE),
                Map.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE, Map.of(
                        "resViolationStatus", "",
                        "resUseType", "아파트",
                        "rows", List.of(
                                Map.of("resFloor", "3층"),
                                Map.of("resFloor", "12층")
                        )
                )),
                "APARTMENT",
                "공동주택, 아파트"
        );

        assertNull(result.getTransactionFloor());
    }

    @Test
    void doesNotTurnAnUnparseableStairwellUseIntoResidentialFromHubFallback() {
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE),
                Map.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE, Map.of(
                        "resViolationStatus", "",
                        "resUseType", "계단실"
                )),
                "APARTMENT",
                "공동주택, 아파트"
        );

        assertEquals("계단실", result.getBuildingData().getBuildingUse());
    }

    @Test
    void missingCollectiveTargetUseDoesNotBecomeSafeFromResidentialHubUse() {
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE),
                Map.of(BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE, Map.of(
                        "resViolationStatus", "",
                        "resExclusiveArea", "84"
                )),
                "APARTMENT",
                "공동주택, 아파트"
        );

        assertNull(result.getBuildingData().getBuildingUse());
    }

    @Test
    void keepsBuildingLevelNonResidentialUseOutOfTheTargetUse() {
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.GENERAL),
                Map.of(BuildingRegisterDocumentType.GENERAL, Map.of(
                        "resViolationStatus", "",
                        "resUseType", "다가구주택"
                )),
                "MULTI_FAMILY",
                "제2종근린생활시설, 다가구주택"
        );

        // 표제부의 근린생활시설을 계약 대상 용도에 합치면 1층에 상가가 있는
        // 정상 매물까지 비주거로 확정되어 DANGER가 된다. 근거로만 남긴다.
        assertEquals("다가구주택", result.getBuildingData().getBuildingUse());
        assertEquals(
                "제2종근린생활시설",
                result.getBuildingData().getBuildingLevelNonResidentialUses()
        );
    }

    @Test
    void doesNotRepeatUnitLevelNonResidentialUseAsBuildingLevelEvidence() {
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.GENERAL),
                Map.of(BuildingRegisterDocumentType.GENERAL, Map.of(
                        "resViolationStatus", "",
                        "resUseType", "제2종근린생활시설"
                )),
                "MULTI_FAMILY",
                "제2종근린생활시설, 다가구주택"
        );

        assertEquals("제2종근린생활시설", result.getBuildingData().getBuildingUse());
        assertNull(result.getBuildingData().getBuildingLevelNonResidentialUses());
    }

    @Test
    void blankStatusMeansNormalOnlyWhenEveryRequiredDocumentSucceeded() {
        List<BuildingRegisterDocumentType> required = List.of(
                BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE
        );

        BuildingRegisterAnalysisData verified = parser.parse(
                required,
                Map.of(
                        BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                        Map.of("resViolationStatus", ""),
                        BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE,
                        Map.of("resViolationStatus", "")
                ),
                "APARTMENT",
                "공동주택"
        );
        BuildingRegisterAnalysisData incomplete = parser.parse(
                required,
                Map.of(BuildingRegisterDocumentType.COLLECTIVE_TITLE, Map.of()),
                "APARTMENT",
                "공동주택"
        );

        assertFalse(verified.getBuildingData().getIsIllegalBuilding());
        assertNull(incomplete.getBuildingData().getIsIllegalBuilding());
    }

    @Test
    void missingViolationStatusIsNotVerified() {
        List<BuildingRegisterDocumentType> required = List.of(
                BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE
        );

        BuildingRegisterAnalysisData result = parser.parse(
                required,
                Map.of(
                        BuildingRegisterDocumentType.COLLECTIVE_TITLE, Map.of(),
                        BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE,
                        Map.of("resUseType", "공동주택")
                ),
                "APARTMENT",
                "공동주택"
        );

        assertNull(result.getBuildingData().getIsIllegalBuilding());
        assertFalse(result.getBuildingData().getIllegalBuildingVerified());
        assertNull(result.getViolationByDocument().get("COLLECTIVE_TITLE"));
        assertNull(result.getViolationByDocument().get("COLLECTIVE_EXCLUSIVE"));
    }
}
