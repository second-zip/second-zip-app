package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.external.BuildingData;
import com.secondzip.backend.report.dto.external.BuildingRegisterAnalysisData;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
public class BuildingRegisterDataParser {
    private static final List<String> DATE_KEYS = List.of(
            "resBaseDate", "resStdDay", "resDate", "resBaseYm", "resBaseYear",
            "resReferenceDate"
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
        boolean exclusiveHasBasePrice = containsKey(targetScope, "resBasePrice");
        Map<String, Object> rawTitleDoc =
                document(documents, BuildingRegisterDocumentType.COLLECTIVE_TITLE);
        TargetScope titleScope = null;
        if (collective && !exclusiveHasBasePrice) {
            // 표제부는 건물 전체 정보만 담아 호 단위 구분이 원래 없는 문서.
            // 동/호 식별 필드가 하나도 없으면(=섞일 후보 자체가 없으면) 통째로
            // 신뢰해도 안전하므로 emptyCandidatesAreSafe=true로 호출한다.
            titleScope = selectTargetScope(rawTitleDoc, detailAddress, true);
            if (titleScope.identityVerified()) {
                officialPrice = findLatestPrice(titleScope.data());
            }
        }
        if (collective && officialPrice == null) {
            // findLatestPrice 내부 경고는 resBasePrice 항목이 하나라도 있을 때만
            // 남는다. 전유부에 그 항목 자체가 없고(오피스텔 등에서 흔함) 표제부에서도
            // 동/호를 특정하지 못하면 두 시도 모두 조용히 null만 반환해 아무 로그도
            // 남지 않는다. "항목이 없어서"인지 "동/호를 못 찾아서"인지 구분해야
            // 실제 데이터 부재인지 파싱 버그인지 판단할 수 있으므로 여기서 남긴다.
            // 표제부에 실제로 어떤 동/호 값이 들어있는지도 같이 남겨야, 호 단위
            // 매칭이 구조적으로 불가능한 문서인지 아니면 다른 이유로 안 맞는지
            // 구분할 수 있다.
            log.warn(
                    "공시가격을 확정하지 못했습니다(전유부·표제부 모두 실패): buildingType={},"
                            + " 전유부 resBasePrice 존재={}, 표제부 문서 존재={}, 표제부 resBasePrice 존재={},"
                            + " 표제부 동/호 식별 성공={}, 표제부 식별 후보={}, detailAddress={}",
                    buildingType,
                    exclusiveHasBasePrice,
                    rawTitleDoc != null,
                    rawTitleDoc != null && containsKey(rawTitleDoc, "resBasePrice"),
                    titleScope != null && titleScope.identityVerified(),
                    rawTitleDoc != null ? describeIdentityCandidates(rawTitleDoc) : "[]",
                    detailAddress
            );
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
        if (transactionAreaSqm == null) {
            // 면적을 못 읽으면 실거래가를 지번·층만으로 맞춰야 해서 정확도가 떨어진다.
            // 어느 키를 봤고 응답에 실제로 어떤 키가 있었는지 남겨 두어야
            // 후보 목록에 무엇을 추가할지 판단할 수 있다.
            log.warn(
                    "대상 면적을 확정하지 못했습니다: collective={}, buildingType={},"
                            + " 찾아본 키={}, 응답에 있는 면적 후보={}, resArea 항목 상세={}",
                    collective,
                    buildingType,
                    collective ? EXCLUSIVE_AREA_KEYS : TOTAL_AREA_KEYS,
                    describeAreaCandidates(targetScope),
                    describeEntriesContaining(targetScope, "resArea")
            );
        }
        Integer transactionFloor = collective ? findFloor(targetScope) : null;
        if (collective && transactionFloor == null) {
            // 층을 못 읽으면 집합건물 실거래가를 지번만으로 맞출 수 없어 매칭을 포기한다.
            // 어느 키를 봤고 응답에 실제로 어떤 키가 있었는지 남겨 두어야
            // 후보 목록에 무엇을 추가할지 판단할 수 있다.
            log.warn(
                    "대상 층을 확정하지 못했습니다: buildingType={}, 찾아본 키={},"
                            + " 응답에 있는 층 후보={}, resFloor 항목 상세={}",
                    buildingType,
                    FLOOR_KEYS,
                    describeFloorCandidates(targetScope),
                    describeEntriesContaining(targetScope, "resFloor")
            );
        }

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
        return selectTargetScope(root, detailAddress, false);
    }

    /**
     * emptyCandidatesAreSafe: 문서에 동/호 식별 필드가 하나도 없으면 애초에 여러
     * 호실 값이 섞여 들어올 방법이 없어(구분할 필드 자체가 없음) 통째로 신뢰해도
     * 안전하다. 표제부처럼 호 단위 구분이 원래 없는 문서 종류에서만 true로 켠다.
     * 전유부처럼 호별로 다른 값이 섞여 들어올 수 있는 문서는 계속 false로 막는다.
     */
    private TargetScope selectTargetScope(
            Object root,
            String detailAddress,
            boolean emptyCandidatesAreSafe
    ) {
        if (root == null || detailAddress == null || detailAddress.isBlank()) {
            return new TargetScope(root, false);
        }
        String expectedDong = normalizeUnit(extractDetailPart(detailAddress, "동"));
        String expectedHo = normalizeUnit(extractDetailPart(detailAddress, "호"));
        List<IdentityCandidate> candidates = new ArrayList<>();
        collectIdentityCandidates(root, null, null, candidates);
        if (candidates.isEmpty()) return new TargetScope(root, emptyCandidatesAreSafe);

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
        Long result = resolveLatestPrice(entries);
        if (result == null && !entries.isEmpty()) {
            // 동/호로 좁혀지지 않은 응답에는 건물 전체 호실의 공시가격이 같은
            // 기준일에 서로 다른 금액으로 섞여 들어온다(대부분 공동주택
            // 공시가격은 단지 전체가 같은 기준일로 고시되기 때문). 어떤
            // 항목들이 후보로 잡혔는지 남겨야 실제로 어떤 필드로 호실을
            // 구분해야 하는지 판단할 수 있다.
            log.warn(
                    "공시가격을 확정하지 못했습니다: resBasePrice 항목 상세={}",
                    describeEntriesContaining(root, "resBasePrice")
            );
        }
        return result;
    }

    private Long resolveLatestPrice(List<Map<String, Object>> entries) {
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
        if (candidates.isEmpty()) return null;

        LinkedHashSet<BigDecimal> allValues = new LinkedHashSet<>();
        for (Map<String, Object> candidate : candidates) {
            BigDecimal value = parseDecimal(candidate.get("resArea"));
            if (value == null) return null;
            allValues.add(value.stripTrailingZeros());
        }
        // 공용면적을 제외한 나머지 후보가 전부 같은 값으로 수렴하면, 전유 표식이나
        // 동·호 귀속이 따로 없어도 그 값 하나로 좁혀진 것이므로 채택한다.
        // (예: 같은 전유부 안에 동일한 면적이 요약행·상세행으로 중복 노출되는 응답)
        if (allValues.size() == 1) return allValues.iterator().next();

        // 값이 서로 다르면 전유 표식이 있거나 동·호에 직접 귀속된 후보만 신뢰한다.
        LinkedHashSet<BigDecimal> trusted = new LinkedHashSet<>();
        for (Map<String, Object> candidate : candidates) {
            boolean boundToUnit = firstDirectValue(candidate, DONG_KEYS) != null
                    || firstDirectValue(candidate, HO_KEYS) != null;
            if (boundToUnit || isExclusiveArea(candidate)) {
                BigDecimal value = parseDecimal(candidate.get("resArea"));
                if (value != null) trusted.add(value.stripTrailingZeros());
            }
        }
        return trusted.size() == 1 ? trusted.iterator().next() : null;
    }

    /**
     * 응답에서 이름에 area가 들어간 키와 값을 모아 보여준다.
     * 면적 확정에 실패했을 때 어떤 키를 후보에 넣어야 하는지 판단하는 근거다.
     */
    private String describeAreaCandidates(Object root) {
        Map<String, List<String>> found = new LinkedHashMap<>();
        collectCandidatesByKeyword(root, "area", found);
        return found.isEmpty() ? "없음" : found.toString();
    }

    private String describeFloorCandidates(Object root) {
        Map<String, List<String>> found = new LinkedHashMap<>();
        collectCandidatesByKeyword(root, "floor", found);
        collectCandidatesByKeyword(root, "flr", found);
        return found.isEmpty() ? "없음" : found.toString();
    }

    /**
     * 응답에 실제로 어떤 동/호 식별자 조합이 들어 있는지 보여준다. 공시가격을
     * 표제부에서 못 찾았을 때, 그 문서가 애초에 호 단위 식별자를 안 가진 것인지
     * (구조적 한계) 아니면 다른 동/호로 채워져 있어서 안 맞은 것인지를 로그만
     * 보고 구분하기 위한 진단용이다.
     */
    private String describeIdentityCandidates(Object root) {
        List<IdentityCandidate> candidates = new ArrayList<>();
        collectIdentityCandidates(root, null, null, candidates);
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        for (IdentityCandidate candidate : candidates) {
            labels.add("동=" + candidate.dong() + ",호=" + candidate.ho());
            if (labels.size() >= 20) break;
        }
        return labels.isEmpty() ? "없음" : labels.toString();
    }

    /**
     * 응답에서 이름에 주어진 키워드가 들어간 필드와 값을 모아 보여준다.
     * 후보 목록에 추가해야 할 실제 키 이름을 찾을 때 쓰는 진단용 로그다.
     * 같은 키가 여러 번 나오면 값을 전부(최대 5개) 남겨서, 값이 하나로
     * 수렴하는지 갈리는지도 로그만으로 판단할 수 있게 한다.
     */
    private void collectCandidatesByKeyword(
            Object node,
            String keyword,
            Map<String, List<String>> destination
    ) {
        if (destination.size() >= 20) {
            return;
        }
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object value = entry.getValue();
                if (key.toLowerCase(Locale.ROOT).contains(keyword)
                        && value != null && !(value instanceof Map)
                        && !(value instanceof Collection)) {
                    List<String> values = destination.computeIfAbsent(
                            key, k -> new ArrayList<>()
                    );
                    String text = value.toString();
                    if (values.size() < 5 && !values.contains(text)) {
                        values.add(text);
                    }
                }
                collectCandidatesByKeyword(value, keyword, destination);
            }
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(value ->
                    collectCandidatesByKeyword(value, keyword, destination)
            );
        }
    }

    /**
     * 주어진 키를 직접 가진 맵들을 찾아, 각 맵의 나머지 필드(값이 스칼라인 것만)를
     * 통째로 보여준다. resArea/resFloor 후보가 여러 개 나올 때, 그중 어느 것이
     * 대상 호실 것인지 구분할 실마리(동·호 키, 전유/공용 구분 코드 등)가 실제로
     * 어떤 이름으로 오는지는 후보 목록·값만 봐서는 알 수 없어서 필요하다.
     */
    private String describeEntriesContaining(Object root, String key) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        collectMapsContainingKey(root, key, candidates);
        if (candidates.isEmpty()) return "없음";
        StringBuilder result = new StringBuilder();
        int limit = Math.min(candidates.size(), 6);
        for (int i = 0; i < limit; i++) {
            if (i > 0) result.append(" | ");
            result.append(scalarFieldsOf(candidates.get(i)));
        }
        if (candidates.size() > limit) {
            result.append(" ... 외 ").append(candidates.size() - limit).append("건");
        }
        return result.toString();
    }

    private Map<String, Object> scalarFieldsOf(Map<String, Object> map) {
        Map<String, Object> scalars = new LinkedHashMap<>();
        map.forEach((k, v) -> {
            if (v != null && !(v instanceof Map) && !(v instanceof Collection)) {
                scalars.put(k, v);
            }
        });
        return scalars;
    }

    /**
     * 전유부 응답 중에는 문자 표식(resExposPubuseGbCdNm 등) 없이 resType
     * 코드 하나로만 전유/공용을 구분하는 경우가 있다. 실제 로그로 확인한
     * 패턴: 대상 호실 자기 행은 resType=0, 그 호실에 딸린 공용부분
     * 배분 행(전기실·계단실·주차장 등)은 전부 resType=1로 내려온다.
     */
    private boolean isExclusiveArea(Map<String, Object> entry) {
        if ("0".equals(typeCode(entry))) return true;
        String marker = areaMarker(entry);
        return marker.contains("전유") || marker.contains("전용");
    }

    private boolean isCommonArea(Map<String, Object> entry) {
        if ("1".equals(typeCode(entry))) return true;
        String marker = areaMarker(entry);
        return marker.contains("공용") || marker.contains("계단실")
                || marker.contains("복도") || marker.contains("승강기")
                || marker.contains("주차장") || marker.contains("기계실")
                || marker.contains("전기실") || marker.contains("관리실")
                || marker.contains("경비실");
    }

    private String typeCode(Map<String, Object> entry) {
        Object value = entry.get("resType");
        return value != null ? value.toString().trim() : null;
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
        // 대상 호실로 좁혀지지 않은 응답에는 건물 전체의 층별 현황(공용부분이
        // 배분된 지하 설비실·계단실·주차장 등)이 같은 키로 섞여 나온다.
        // 값만 모으면 항상 여러 층이 갈려 보이므로, 전유 표식이 있는 행을
        // 먼저 가려낸 뒤에야 값이 하나로 수렴하는지 판단할 수 있다.
        for (String key : FLOOR_KEYS) {
            FloorSelection resolved = selectFloorFromCandidates(root, key);
            if (resolved.present()) return resolved.value();
        }
        return null;
    }

    private FloorSelection selectFloorFromCandidates(Object root, String key) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        collectMapsContainingKey(root, key, candidates);
        if (candidates.isEmpty()) return FloorSelection.absent();

        List<Map<String, Object>> nonCommon = candidates.stream()
                .filter(candidate -> !isCommonArea(candidate))
                .toList();
        if (nonCommon.isEmpty()) return new FloorSelection(true, null);

        LinkedHashSet<Integer> allFloors = new LinkedHashSet<>();
        for (Map<String, Object> candidate : nonCommon) {
            Integer floor = parseFloor(textValue(candidate.get(key)));
            if (floor == null) return new FloorSelection(true, null);
            allFloors.add(floor);
        }
        if (allFloors.size() == 1) return new FloorSelection(true, allFloors.iterator().next());

        LinkedHashSet<Integer> trusted = new LinkedHashSet<>();
        for (Map<String, Object> candidate : nonCommon) {
            boolean boundToUnit = firstDirectValue(candidate, DONG_KEYS) != null
                    || firstDirectValue(candidate, HO_KEYS) != null;
            if (boundToUnit || isExclusiveArea(candidate)) {
                Integer floor = parseFloor(textValue(candidate.get(key)));
                if (floor != null) trusted.add(floor);
            }
        }
        return new FloorSelection(true, trusted.size() == 1 ? trusted.iterator().next() : null);
    }

    private String textValue(Object raw) {
        return raw != null ? raw.toString() : null;
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
