package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.external.BuildingRegisterAnalysisData;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import org.junit.jupiter.api.Test;

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
                List.of(BuildingRegisterDocumentType.COLLECTIVE_TITLE),
                Map.of(
                        BuildingRegisterDocumentType.COLLECTIVE_TITLE,
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
    void fallsBackToLargestAmountWhenNoBaseDateExists() {
        BuildingRegisterAnalysisData result = parser.parse(
                List.of(BuildingRegisterDocumentType.COLLECTIVE_TITLE),
                Map.of(
                        BuildingRegisterDocumentType.COLLECTIVE_TITLE,
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

        assertEquals(450_000_000L, result.getOfficialPrice());
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
