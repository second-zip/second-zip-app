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
                officialPrice = findLatestBasePrice(data);
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

    /**
     * 공시가격(주택가격)을 고른다. <b>기준일이 가장 최근인 값</b>을 쓴다.
     *
     * <p>건축물대장 주택가격은 연도별 이력으로 여러 건이 내려온다. 예전에는 그중
     * 가장 큰 금액을 골랐는데, 공시가격이 하락한 해가 있으면 과거의 높은 값을 쓰게 된다.
     * 기준가가 실제보다 높아지면 전세가율이 낮게 계산되어 <b>위험을 과소평가</b>한다.
     *
     * <p>다만 CODEF 응답에서 기준일 필드명을 확정하지 못해, 날짜로 보이는 필드를
     * 폭넓게 찾는다. 끝내 날짜를 찾지 못하면 기존 동작(최대 금액)으로 물러난다.
     * 실제 응답을 확인해 필드명이 확정되면 {@link #DATE_KEY_CANDIDATES} 맨 앞에 두면 된다.
     */
    private Long findLatestBasePrice(Object data) {
        List<Map<String, Object>> entries = new java.util.ArrayList<>();
        collectMapsContainingKey(data, "resBasePrice", entries);

        Long latestPrice = null;
        String latestDate = null;

        for (Map<String, Object> entry : entries) {
            Long price = parseAmount(entry.get("resBasePrice"));
            if (price == null) {
                continue;
            }
            String date = findEntryDate(entry);
            if (date == null) {
                continue;
            }
            if (latestDate == null || date.compareTo(latestDate) > 0) {
                latestDate = date;
                latestPrice = price;
            }
        }

        if (latestPrice != null) {
            return latestPrice;
        }
        // 기준일을 찾지 못한 경우에만 기존 방식으로 폴백한다.
        return findLargestAmount(findTextValues(data, "resBasePrice"));
    }

    /** 실제 응답에서 필드명이 확인되면 맨 앞에 추가한다. */
    private static final List<String> DATE_KEY_CANDIDATES = List.of(
            "resBaseDate", "resStdDay", "resDate", "resBaseYm", "resBaseYear"
    );

    /** 가격 항목에 붙은 기준일을 yyyyMMdd 8자리로 정규화해 돌려준다. */
    private String findEntryDate(Map<String, Object> entry) {
        for (String key : DATE_KEY_CANDIDATES) {
            String normalized = normalizeDate(entry.get(key));
            if (normalized != null) {
                return normalized;
            }
        }
        // 후보에 없으면 이름이 날짜처럼 보이는 필드를 찾아본다.
        for (Map.Entry<String, Object> field : entry.entrySet()) {
            String key = field.getKey() == null
                    ? ""
                    : field.getKey().toLowerCase();
            if (key.contains("date") || key.contains("day")
                    || key.contains("ym") || key.contains("year")) {
                String normalized = normalizeDate(field.getValue());
                if (normalized != null) {
                    return normalized;
                }
            }
        }
        return null;
    }

    /**
     * "2024-01-01" / "20240101" / "202401" / "2024" 를 모두 8자리로 맞춘다.
     * 자릿수를 맞춰야 문자열 비교로 최신 여부를 판단할 수 있다.
     */
    private String normalizeDate(Object raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.toString().replaceAll("[^0-9]", "");
        if (digits.length() < 4 || digits.length() > 8) {
            return null;
        }
        if (!digits.startsWith("19") && !digits.startsWith("20")) {
            return null; // 연도로 보이지 않으면 날짜가 아니다
        }
        return (digits + "00000000").substring(0, 8);
    }

    private Long parseAmount(Object raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.toString().replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 지정한 키를 <b>직접</b> 가지고 있는 맵들을 모은다. 가격과 기준일을 같이 보기 위해 필요하다. */
    @SuppressWarnings("unchecked")
    private void collectMapsContainingKey(
            Object node,
            String targetKey,
            List<Map<String, Object>> destination
    ) {
        if (node instanceof Map<?, ?> map) {
            if (map.containsKey(targetKey)) {
                destination.add((Map<String, Object>) map);
            }
            map.values().forEach(value ->
                    collectMapsContainingKey(value, targetKey, destination)
            );
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(value ->
                    collectMapsContainingKey(value, targetKey, destination)
            );
        }
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
