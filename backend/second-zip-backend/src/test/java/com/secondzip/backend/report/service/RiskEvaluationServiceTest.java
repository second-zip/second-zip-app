package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.CheckResult;
import com.secondzip.backend.report.dto.RiskEvaluationResult;
import com.secondzip.backend.report.dto.external.BuildingData;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
