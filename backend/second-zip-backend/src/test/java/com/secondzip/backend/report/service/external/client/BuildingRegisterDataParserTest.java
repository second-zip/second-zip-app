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
