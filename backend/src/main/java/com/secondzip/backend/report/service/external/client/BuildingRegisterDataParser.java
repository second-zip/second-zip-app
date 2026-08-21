package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.external.BuildingData;
import com.secondzip.backend.report.dto.external.BuildingRegisterAnalysisData;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BuildingRegisterDataParser {
    private static final List<String> DATE_KEYS = List.of(
            "resBaseDate", "resStdDay", "resDate", "resBaseYm", "resBaseYear"
    );
    private static final List<String> DONG_KEYS = List.of(
            "resDong", "resDongNm", "resDongName", "commDongNum", "dong", "dongNm"
    );
    private static final List<String> HO_KEYS = List.of(
            "resHo", "resHoNm", "resHoName", "commHoNum", "ho", "hoNm"
    );
    private static final List<String> EXCLUSIVE_AREA_KEYS = List.of(
            "resExclusiveArea", "resExclusiveAreaM2", "resExclusiveUseArea",
            "resExclusiveUseAr", "resExcluUseArea", "resExcluUseAr", "resExposArea"
    );
    private static final List<String> TOTAL_AREA_KEYS = List.of(
            "resTotalFloorArea", "resTotalFloorAr", "resTotalArea", "resTotArea",
            "resFloorAreaTotal", "totArea"
    );
    private static final List<String> FLOOR_KEYS = List.of(
            "resFloor", "resFloorNo", "resFloorNm", "floor", "floorNo"
    );
    private static final List<String> NON_RESIDENTIAL_MARKERS = List.of(
            "비주거", "업무용", "근린생활시설", "업무시설", "숙박시설",
            "판매시설", "위락시설", "공장", "창고시설", "의료시설",
            "교육연구시설", "노유자시설", "종교시설", "자동차관련시설", "상가"
    );
    private static final Pattern NUMBER = Pattern.compile(
            "[-+]?[0-9][0-9,]*(?:\\.[0-9]+)?"
    );
    public BuildingRegisterAnalysisData parse(
            List<BuildingRegisterDocumentType> requiredDocuments,
            Map<BuildingRegisterDocumentType, Map<String, Object>> documents,
            String buildingType,
            String fallbackBuildingUse
    ) {
        return parse(requiredDocuments, documents, buildingType,
                fallbackBuildingUse, null, null);
    }

    public BuildingRegisterAnalysisData parse(
            List<BuildingRegisterDocumentType> requiredDocuments,
            Map<BuildingRegisterDocumentType, Map<String, Object>> documents,
            String buildingType,
            String fallbackBuildingUse,
            String detailAddress
    ) {
        return parse(requiredDocuments, documents, buildingType,
                fallbackBuildingUse, detailAddress, null);
    }

    public BuildingRegisterAnalysisData parse(
            List<BuildingRegisterDocumentType> requiredDocuments,
            Map<BuildingRegisterDocumentType, Map<String, Object>> documents,
            String buildingType,
            String fallbackBuildingUse,
            String detailAddress,
            BigDecimal fallbackTransactionAreaSqm
    ) {
        boolean collective = isCollective(buildingType);
        TargetScope exclusiveScope = collective
                ? selectTargetScope(
                document(documents, BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE),
                detailAddress
        )
                : new TargetScope(
                document(documents, BuildingRegisterDocumentType.GENERAL),
                false
        );
        Object targetScope = exclusiveScope.data();

        Map<String, Boolean> violations = new LinkedHashMap<>();
        boolean verified = requiredDocuments != null && !requiredDocuments.isEmpty();
        boolean anyViolation = false;
        if (requiredDocuments != null) {
            for (BuildingRegisterDocumentType type : requiredDocuments) {
                Object source = collective
                        && type == BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE
                        ? targetScope
                        : document(documents, type);
                List<String> statuses = findValues(
                        source,
                        "resViolationStatus",
                        true
                );
                if (source == null || statuses.isEmpty()
                        || statuses.stream().allMatch(Objects::isNull)) {
                    verified = false;
                    violations.put(type.name(), null);
                    continue;
                }
                boolean violation = statuses.stream()
                        .filter(Objects::nonNull)
                        .anyMatch(value -> value.contains("위반건축물"));
                violations.put(type.name(), violation);
                anyViolation |= violation;
            }
        }

        String use = combineTargetUses(targetScope);
        Long officialPrice = findLatestPrice(targetScope);
        if (collective && !containsKey(targetScope, "resBasePrice")) {
            TargetScope titleScope = selectTargetScope(
                    document(documents, BuildingRegisterDocumentType.COLLECTIVE_TITLE),
                    detailAddress
            );
            if (titleScope.identityVerified()) {
                officialPrice = findLatestPrice(titleScope.data());
            }
        }
        DecimalSelection totalArea = collective
                ? DecimalSelection.absent()
                : selectDecimalByPriority(targetScope, TOTAL_AREA_KEYS);
        BigDecimal transactionAreaSqm = collective
                ? findExclusiveArea(targetScope)
                : totalArea.value();
        if (!collective && !totalArea.present()) {
            transactionAreaSqm = positive(fallbackTransactionAreaSqm);
        }
        Integer transactionFloor = collective ? findFloor(targetScope) : null;

        BuildingData buildingData = new BuildingData();
        buildingData.setBuildingType(buildingType);
        String targetUse = resolveTargetUse(use, fallbackBuildingUse, !collective);
        buildingData.setBuildingUse(targetUse);
        buildingData.setBuildingLevelNonResidentialUses(
                buildingLevelNonResidentialUses(targetUse, use, fallbackBuildingUse)
        );
        buildingData.setIsIllegalBuilding(verified ? anyViolation : null);
        buildingData.setIllegalBuildingVerified(verified);
        buildingData.setIllegalBuildingSource("CODEF_BUILDING_REGISTER");
        buildingData.setViolationByDocument(new LinkedHashMap<>(violations));
        buildingData.setTransactionAreaSqm(transactionAreaSqm);
        return new BuildingRegisterAnalysisData(
                buildingData,
                officialPrice,
                violations,
                transactionAreaSqm,
                transactionFloor
        );
    }

    private Map<String, Object> document(
            Map<BuildingRegisterDocumentType, Map<String, Object>> documents,
            BuildingRegisterDocumentType type
    ) {
        return documents != null ? documents.get(type) : null;
    }

    private boolean isCollective(String type) {
        return "APARTMENT".equals(type)
                || "MULTI_HOUSEHOLD".equals(type)
                || "OFFICETEL".equals(type);
    }

    /** 응답에 동·호 식별자가 있으면 요청 상세주소와 일치하는 가장 구체적인 맵만 남긴다. */
    private TargetScope selectTargetScope(Object root, String detailAddress) {
        if (root == null || detailAddress == null || detailAddress.isBlank()) {
            return new TargetScope(root, false);
        }
        String expectedDong = normalizeUnit(extractDetailPart(detailAddress, "동"));
        String expectedHo = normalizeUnit(extractDetailPart(detailAddress, "호"));
        List<IdentityCandidate> candidates = new ArrayList<>();
        collectIdentityCandidates(root, null, null, candidates);
        if (candidates.isEmpty()) return new TargetScope(root, false);

        List<IdentityCandidate> selectedCandidates = candidates.stream()
                .filter(candidate -> expectedDong == null
                        || expectedDong.equals(candidate.dong()))
                .filter(candidate -> expectedHo == null
                        || expectedHo.equals(candidate.ho()))
                .toList();
        if (selectedCandidates.isEmpty()) return new TargetScope(null, false);

        LinkedHashSet<UnitIdentity> identities = selectedCandidates.stream()
                .map(candidate -> new UnitIdentity(candidate.dong(), candidate.ho()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (identities.size() != 1
                || expectedDong == null
                && selectedCandidates.size() > 1
                && selectedCandidates.stream().allMatch(candidate -> candidate.dong() == null)) {
            return new TargetScope(null, false);
        }
        List<Map<String, Object>> selected = selectedCandidates.stream()
                .map(IdentityCandidate::data)
                .distinct()
                .toList();
        return new TargetScope(
                selected.size() == 1 ? selected.get(0) : selected,
                expectedHo != null
        );
    }

    private void collectIdentityCandidates(
            Object node,
            String inheritedDong,
            String inheritedHo,
            List<IdentityCandidate> destination
    ) {
        if (node instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = castMap(rawMap);
            String directDong = normalizeUnit(firstDirectValue(map, DONG_KEYS));
            String directHo = normalizeUnit(firstDirectValue(map, HO_KEYS));
            String currentDong = directDong != null ? directDong : inheritedDong;
            String currentHo = directHo != null ? directHo : inheritedHo;
            if (directDong != null || directHo != null) {
                destination.add(new IdentityCandidate(map, currentDong, currentHo));
            }
            map.values().forEach(value -> collectIdentityCandidates(
                    value,
                    currentDong,
                    currentHo,
                    destination
            ));
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(value -> collectIdentityCandidates(
                    value,
                    inheritedDong,
                    inheritedHo,
                    destination
            ));
        }
    }

    private String firstDirectValue(Map<String, Object> map, List<String> keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !value.toString().isBlank()) return value.toString();
        }
        return null;
    }

    private String extractDetailPart(String detailAddress, String suffix) {
        Matcher matcher = Pattern.compile(
                "([^\\s,]+)\\s*" + Pattern.quote(suffix)
        ).matcher(detailAddress);
        String found = null;
        while (matcher.find()) found = matcher.group(1);
        return found;
    }

    private String normalizeUnit(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String normalized = raw.replaceAll("\\s+", "")
                .replaceFirst("^제", "")
                .replaceFirst("[동호]$", "")
                .toUpperCase(Locale.ROOT);
        if (normalized.matches("[0-9]+")) {
            try {
                return Long.toString(Long.parseLong(normalized));
            } catch (NumberFormatException ignored) {
                // 숫자가 지나치게 길면 원문 비교를 유지한다.
            }
        }
        return normalized;
    }

    /** 날짜 없는 서로 다른 가격이 있으면 최신값을 증명할 수 없어 null이다. */
    private Long findLatestPrice(Object root) {
        List<Map<String, Object>> entries = new ArrayList<>();
        collectMapsContainingKey(root, "resBasePrice", entries);
        Long latestPrice = null;
        String latestDate = null;
        boolean undated = false;
        Long soleAmount = null;
        boolean amountConflict = false;
        String latestUnreadableDate = null;
        boolean hasUndatedUnreadablePrice = false;
        for (Map<String, Object> entry : entries) {
            Object rawPrice = entry.get("resBasePrice");
            Long price = parseAmount(rawPrice);
            String date = findEntryDate(entry);
            if (price == null) {
                if (rawPrice != null && !rawPrice.toString().isBlank()) {
                    if (date == null) {
                        hasUndatedUnreadablePrice = true;
                    } else if (latestUnreadableDate == null
                            || date.compareTo(latestUnreadableDate) > 0) {
                        latestUnreadableDate = date;
                    }
                }
                continue;
            }
            if (soleAmount == null) {
                soleAmount = price;
            } else if (!soleAmount.equals(price)) {
                amountConflict = true;
            }
            if (date == null) {
                undated = true;
                continue;
            }
            if (latestDate == null || date.compareTo(latestDate) > 0) {
                latestDate = date;
                latestPrice = price;
            } else if (date.equals(latestDate) && !price.equals(latestPrice)) {
                return null;
            }
        }
        if (hasUndatedUnreadablePrice
                || latestUnreadableDate != null
                && (latestDate == null
                || latestUnreadableDate.compareTo(latestDate) >= 0)) {
            return null;
        }
        if (undated) return amountConflict ? null : soleAmount;
        return latestPrice;
    }

    private String findEntryDate(Map<String, Object> entry) {
        for (String key : DATE_KEYS) {
            String date = normalizeDate(entry.get(key));
            if (date != null) return date;
        }
        return null;
    }

    private String normalizeDate(Object raw) {
        if (raw == null) return null;
        String digits = raw.toString().replaceAll("[^0-9]", "");
        try {
            if (digits.matches("(?:19|20)[0-9]{2}")) {
                return digits + "0000";
            }
            if (digits.matches("(?:19|20)[0-9]{4}")) {
                YearMonth.of(
                        Integer.parseInt(digits.substring(0, 4)),
                        Integer.parseInt(digits.substring(4, 6))
                );
                return digits + "00";
            }
            if (digits.matches("(?:19|20)[0-9]{6}")) {
                LocalDate.parse(digits, DateTimeFormatter.BASIC_ISO_DATE);
                return digits;
            }
        } catch (DateTimeException | NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    private Long parseAmount(Object raw) {
        if (raw == null) return null;
        String digits = raw.toString().replaceAll("[^0-9]", "");
        if (digits.isBlank()) return null;
        try {
            return Long.valueOf(digits);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void collectMapsContainingKey(
            Object node,
            String key,
            List<Map<String, Object>> destination
    ) {
        if (node instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = castMap(rawMap);
            if (map.containsKey(key)) destination.add(map);
            map.values().forEach(value ->
                    collectMapsContainingKey(value, key, destination)
            );
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(value ->
                    collectMapsContainingKey(value, key, destination)
            );
        }
    }

    private boolean containsKey(Object root, String key) {
        List<Map<String, Object>> matches = new ArrayList<>();
        collectMapsContainingKey(root, key, matches);
        return !matches.isEmpty();
    }

    private BigDecimal findExclusiveArea(Object root) {
        DecimalSelection explicit = selectDecimalByPriority(root, EXCLUSIVE_AREA_KEYS);
        if (explicit.present()) return explicit.value();

        List<Map<String, Object>> candidates = new ArrayList<>();
        collectMapsContainingKey(root, "resArea", candidates);
        candidates.removeIf(this::isCommonArea);
        LinkedHashSet<BigDecimal> values = new LinkedHashSet<>();
        boolean eligible = false;
        for (Map<String, Object> candidate : candidates) {
            boolean boundToUnit = firstDirectValue(candidate, DONG_KEYS) != null
                    || firstDirectValue(candidate, HO_KEYS) != null;
            if (boundToUnit || isExclusiveArea(candidate) || candidates.size() == 1) {
                eligible = true;
                BigDecimal value = parseDecimal(candidate.get("resArea"));
                if (value == null) return null;
                values.add(value.stripTrailingZeros());
            }
        }
        return eligible && values.size() == 1 ? values.iterator().next() : null;
    }

    private boolean isExclusiveArea(Map<String, Object> entry) {
        String marker = areaMarker(entry);
        return marker.contains("전유") || marker.contains("전용");
    }

    private boolean isCommonArea(Map<String, Object> entry) {
        String marker = areaMarker(entry);
        return marker.contains("공용") || marker.contains("계단실")
                || marker.contains("복도") || marker.contains("승강기")
                || marker.contains("주차장") || marker.contains("기계실")
                || marker.contains("전기실") || marker.contains("관리실")
                || marker.contains("경비실");
    }

    private String areaMarker(Map<String, Object> entry) {
        StringBuilder marker = new StringBuilder();
        for (String key : List.of(
                "resUseType", "resType1", "resType2", "resAreaType",
                "resExposPubuseGbCdNm"
        )) {
            Object value = entry.get(key);
            if (value != null) marker.append(value);
        }
        return marker.toString().replaceAll("\\s+", "");
    }

    private DecimalSelection selectDecimalByPriority(Object root, List<String> keys) {
        for (String key : keys) {
            List<String> rawValues = findValues(root, key, false);
            if (rawValues.isEmpty()) continue;
            LinkedHashSet<BigDecimal> values = new LinkedHashSet<>();
            for (String value : rawValues) {
                BigDecimal parsed = parseDecimal(value);
                if (parsed == null) return new DecimalSelection(true, null);
                values.add(parsed.stripTrailingZeros());
            }
            return new DecimalSelection(
                    true,
                    values.size() == 1 ? values.iterator().next() : null
            );
        }
        return DecimalSelection.absent();
    }

    private BigDecimal parseDecimal(Object raw) {
        if (raw == null) return null;
        Matcher matcher = NUMBER.matcher(raw.toString());
        if (!matcher.find()) return null;
        try {
            return positive(new BigDecimal(matcher.group().replace(",", "")));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal positive(BigDecimal value) {
        return value != null && value.signum() > 0 ? value : null;
    }

    private Integer findFloor(Object root) {
        for (String key : FLOOR_KEYS) {
            FloorSelection direct = selectFloorValues(directValues(root, key));
            if (direct.present()) return direct.value();
        }
        for (String key : FLOOR_KEYS) {
            FloorSelection recursive = selectFloorValues(findValues(root, key, false));
            if (recursive.present()) return recursive.value();
        }
        return null;
    }

    private List<String> directValues(Object root, String key) {
        List<String> values = new ArrayList<>();
        if (root instanceof Map<?, ?> map) {
            Object value = map.get(key);
            if (value != null && !value.toString().isBlank()) {
                values.add(value.toString());
            }
        } else if (root instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item instanceof Map<?, ?> map) {
                    Object value = map.get(key);
                    if (value != null && !value.toString().isBlank()) {
                        values.add(value.toString());
                    }
                }
            }
        }
        return values;
    }

    private FloorSelection selectFloorValues(List<String> rawValues) {
        if (rawValues.isEmpty()) return FloorSelection.absent();
        LinkedHashSet<Integer> floors = new LinkedHashSet<>();
        for (String raw : rawValues) {
            Integer floor = parseFloor(raw);
            if (floor == null) return new FloorSelection(true, null);
            floors.add(floor);
        }
        return new FloorSelection(
                true,
                floors.size() == 1 ? floors.iterator().next() : null
        );
    }

    private Integer parseFloor(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.contains("지상") && value.contains("지하")) return null;
        try {
            Matcher basement = Pattern.compile(
                    "(?:B|지하\\s*)([0-9]+)",
                    Pattern.CASE_INSENSITIVE
            ).matcher(value);
            if (basement.find()) return -Integer.parseInt(basement.group(1));
            Matcher number = Pattern.compile("-?[0-9]+").matcher(value);
            return number.find() ? Integer.valueOf(number.group()) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String combineTargetUses(Object targetScope) {
        LinkedHashSet<String> uses = new LinkedHashSet<>();
        for (String key : List.of("resUseType", "resType1")) {
            for (String value : findValues(targetScope, key, false)) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) uses.add(trimmed);
            }
        }
        return uses.isEmpty() ? null : String.join(", ", uses);
    }

    /**
     * 계약 대상 호의 용도.
     *
     * 전유부(집합건물) 또는 일반건축물 표제부에서 읽은 용도를 그대로 쓴다.
     * 건축HUB 표제부 용도는 건물 전체의 주용도라서 이 값에 섞지 않는다.
     * 단독·다가구는 표제부가 곧 계약 대상 건물이므로, 대장에서 용도를 읽지
     * 못했을 때만 HUB 값으로 대체한다.
     */
    private String resolveTargetUse(
            String targetUse,
            String hubUse,
            boolean allowFullHubFallback
    ) {
        LinkedHashSet<String> uses = new LinkedHashSet<>();
        addUseParts(uses, targetUse);
        if (uses.isEmpty() && allowFullHubFallback) {
            addUseParts(uses, hubUse);
        }
        return uses.isEmpty() ? null : String.join(", ", uses);
    }

    /**
     * 표제부에만 나타나는 비주거 용도.
     *
     * 계약 대상 호의 용도로 이미 채택된 값은 제외한다. 같은 문자열이 양쪽에
     * 남으면 리포트에서 같은 사실이 두 번 위험으로 세어진다.
     */
    private String buildingLevelNonResidentialUses(
            String targetUse,
            String registerUse,
            String hubUse
    ) {
        LinkedHashSet<String> adopted = new LinkedHashSet<>();
        addUseParts(adopted, targetUse);

        LinkedHashSet<String> uses = new LinkedHashSet<>();
        addNonResidentialUseParts(uses, registerUse);
        addNonResidentialUseParts(uses, hubUse);
        uses.removeAll(adopted);
        return uses.isEmpty() ? null : String.join(", ", uses);
    }

    private void addUseParts(LinkedHashSet<String> destination, String raw) {
        if (raw == null) return;
        for (String part : raw.split("[,;]")) {
            String value = part.trim();
            if (!value.isEmpty()) destination.add(value);
        }
    }

    private void addNonResidentialUseParts(
            LinkedHashSet<String> destination,
            String raw
    ) {
        if (raw == null) return;
        for (String part : raw.split("[,;]")) {
            String value = part.trim();
            if (NON_RESIDENTIAL_MARKERS.stream().anyMatch(value::contains)) {
                destination.add(value);
            }
        }
    }

    private List<String> findValues(Object root, String key, boolean preserveNull) {
        List<String> values = new ArrayList<>();
        collectValues(root, key, preserveNull, values);
        return values;
    }

    private void collectValues(
            Object node,
            String key,
            boolean preserveNull,
            List<String> destination
    ) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (key.equals(String.valueOf(entry.getKey()))
                        && (preserveNull
                        || value != null && !value.toString().isBlank())) {
                    destination.add(value != null ? value.toString() : null);
                }
                collectValues(value, key, preserveNull, destination);
            }
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(value ->
                    collectValues(value, key, preserveNull, destination)
            );
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private record TargetScope(Object data, boolean identityVerified) {
    }

    private record IdentityCandidate(
            Map<String, Object> data,
            String dong,
            String ho
    ) {
    }

    private record UnitIdentity(String dong, String ho) {
    }

    private record DecimalSelection(boolean present, BigDecimal value) {
        static DecimalSelection absent() {
            return new DecimalSelection(false, null);
        }
    }

    private record FloorSelection(boolean present, Integer value) {
        static FloorSelection absent() {
            return new FloorSelection(false, null);
        }
    }
}
