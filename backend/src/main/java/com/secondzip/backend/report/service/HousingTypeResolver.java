package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.response.CheckResultView;
import com.secondzip.backend.report.enums.CheckType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class HousingTypeResolver {

    private static final String UNKNOWN = "UNKNOWN";

    public String resolve(List<CheckResultView> checkResults) {
        if (checkResults == null || checkResults.isEmpty()) {
            return UNKNOWN;
        }

        return checkResults.stream()
                .filter(Objects::nonNull)
                .filter(checkResult ->
                        checkResult.getCheckType() == CheckType.BUILDING_USE
                )
                .map(CheckResultView::getEvidence)
                .filter(Objects::nonNull)
                .map(evidence -> evidence.get("buildingUse"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(this::resolveFromBuildingUse)
                .filter(result -> !UNKNOWN.equals(result))
                .findFirst()
                .orElse(UNKNOWN);
    }

    private String resolveFromBuildingUse(String buildingUse) {
        if (buildingUse == null || buildingUse.isBlank()) {
            return UNKNOWN;
        }

        String normalized = buildingUse.replaceAll("\\s+", "");

        if (normalized.contains("오피스텔")) {
            return "OFFICETEL";
        }

        if (normalized.contains("다가구")) {
            return "MULTI_FAMILY";
        }

        if (normalized.contains("다세대")
                || normalized.contains("연립")
                || normalized.contains("빌라")) {
            return "MULTI_HOUSEHOLD";
        }

        if (normalized.contains("아파트")) {
            return "APARTMENT";
        }

        if (normalized.contains("단독주택")) {
            return "SINGLE_FAMILY";
        }

        return UNKNOWN;
    }
}
