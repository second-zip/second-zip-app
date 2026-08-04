package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.external.BuildingData;
import com.secondzip.backend.report.dto.external.BuildingRegisterAnalysisData;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class BuildingRegisterDataParser {

    public BuildingRegisterAnalysisData parse(
            List<BuildingRegisterDocumentType> requiredDocuments,
            Map<BuildingRegisterDocumentType, Map<String, Object>> documents,
            String buildingType,
            String fallbackBuildingUse
    ) {
        if (requiredDocuments == null || requiredDocuments.isEmpty()) {
            return unknown(buildingType, fallbackBuildingUse);
        }

        Map<String, Boolean> violationByDocument = new LinkedHashMap<>();
        boolean allDocumentsVerified = true;
        boolean anyViolation = false;
        String buildingUse = null;
        Long officialPrice = null;

        for (BuildingRegisterDocumentType type : requiredDocuments) {
            Map<String, Object> data = documents != null ? documents.get(type) : null;
            if (data == null) {
                allDocumentsVerified = false;
                violationByDocument.put(type.name(), null);
                continue;
            }

            List<String> violationStatuses =
                    findFieldValues(data, "resViolationStatus");
            if (violationStatuses.isEmpty()
                    || violationStatuses.stream().allMatch(
                    java.util.Objects::isNull
            )) {
                allDocumentsVerified = false;
                violationByDocument.put(type.name(), null);
                continue;
            }

            boolean violation = violationStatuses.stream()
                    .filter(java.util.Objects::nonNull)
                    .anyMatch(value -> value.contains("위반건축물"));
            violationByDocument.put(type.name(), violation);
            anyViolation |= violation;

            if (buildingUse == null) {
                buildingUse = firstNonBlank(
                        findTextValues(data, "resUseType"),
                        findTextValues(data, "resType1")
                );
            }
            if (officialPrice == null) {
                officialPrice = findLargestAmount(
                        findTextValues(data, "resBasePrice")
                );
            }
        }

        BuildingData buildingData = new BuildingData();
        buildingData.setBuildingType(buildingType);
        buildingData.setBuildingUse(
                buildingUse != null ? buildingUse : fallbackBuildingUse
        );
        buildingData.setIsIllegalBuilding(
                allDocumentsVerified ? anyViolation : null
        );
        buildingData.setIllegalBuildingVerified(allDocumentsVerified);
        buildingData.setIllegalBuildingSource("CODEF_BUILDING_REGISTER");
        buildingData.setViolationByDocument(
                new LinkedHashMap<>(violationByDocument)
        );
        return new BuildingRegisterAnalysisData(
                buildingData,
                officialPrice,
                violationByDocument
        );
    }

    private BuildingRegisterAnalysisData unknown(
            String buildingType,
            String fallbackBuildingUse
    ) {
        BuildingData buildingData = new BuildingData();
        buildingData.setBuildingType(buildingType);
        buildingData.setBuildingUse(fallbackBuildingUse);
        buildingData.setIsIllegalBuilding(null);
        buildingData.setIllegalBuildingVerified(false);
        buildingData.setIllegalBuildingSource("CODEF_BUILDING_REGISTER");
        buildingData.setViolationByDocument(new LinkedHashMap<>());
        return new BuildingRegisterAnalysisData(
                buildingData,
                null,
                Map.of()
        );
    }

    @SafeVarargs
    private String firstNonBlank(List<String>... groups) {
        for (List<String> group : groups) {
            for (String value : group) {
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    private Long findLargestAmount(List<String> values) {
        Long largest = null;
        for (String value : values) {
            String digits = value != null ? value.replaceAll("[^0-9]", "") : "";
            if (digits.isBlank()) continue;
            try {
                long amount = Long.parseLong(digits);
                if (largest == null || amount > largest) {
                    largest = amount;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return largest;
    }

    private List<String> findTextValues(Object root, String targetKey) {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        collectTextValues(root, targetKey, values);
        return values;
    }

    private List<String> findFieldValues(Object root, String targetKey) {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        collectFieldValues(root, targetKey, values);
        return values;
    }

    private void collectFieldValues(
            Object node,
            String targetKey,
            List<String> destination
    ) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (targetKey.equals(String.valueOf(entry.getKey()))) {
                    destination.add(value != null ? value.toString() : null);
                }
                collectFieldValues(value, targetKey, destination);
            }
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(value ->
                    collectFieldValues(value, targetKey, destination)
            );
        }
    }

    private void collectTextValues(
            Object node,
            String targetKey,
            List<String> destination
    ) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (targetKey.equals(String.valueOf(entry.getKey()))
                        && value != null
                        && !value.toString().isBlank()) {
                    destination.add(value.toString());
                }
                collectTextValues(value, targetKey, destination);
            }
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(value ->
                    collectTextValues(value, targetKey, destination)
            );
        }
    }
}
