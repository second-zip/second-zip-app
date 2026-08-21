package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.external.RegistryData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RegistryDataParser {

    private static final Pattern CANCELLED_SPAN =
            Pattern.compile("&[^&]*&", Pattern.DOTALL);
    private static final Pattern MORTGAGE_AMOUNT =
            Pattern.compile("채권최고액\\s*금?\\s*([0-9][0-9,]*)\\s*원?");
    private static final Pattern MORTGAGE_MARKER = Pattern.compile("채권최고액");
    private static final Pattern OWNER_PREFIX = Pattern.compile(
            "(?<!\\S)(소유자|공유자|수탁자)\\s*[:：]?\\s*([^\\n|]+)"
    );
    private static final Pattern OWNER_SUFFIX_ROLE = Pattern.compile(
            "(?m)^\\s*(.+?)\\s*\\((소유자|공유자|수탁자)\\)\\s*(?:$|[^\\n]*)"
    );
    private static final Pattern OWNER_TRAILING_FIELDS = Pattern.compile(
            "\\s+(?:주민등록번호|법인등록번호|주소|지분|순위번호|등기원인"
                    + "|채권최고액|근저당권|저당권|압류|가압류|가처분|가등기"
                    + "|신탁|경매).*$"
    );
    private static final Pattern ID_NUMBER = Pattern.compile(
            "\\s*\\d{6}\\s*[-*]\\s*[0-9*]{7}(?:\\s.*)?$"
    );
    private static final Pattern TRUST_STANDALONE = Pattern.compile(
            "(?<![가-힣A-Za-z0-9])신탁(?![가-힣A-Za-z0-9])"
    );
    private static final Pattern TRUST_EXPLICIT = Pattern.compile(
            "(?<![가-힣A-Za-z0-9])신탁(?:등기|원부|설정|재산)"
                    + "|등기원인\\s*[:：]?\\s*신탁"
    );
    private static final Pattern TRUST_TERMINATION = Pattern.compile(
            "(?<![가-힣A-Za-z0-9])신탁(?:등기|원부)?\\s*"
                    + "(?:말소|해제|해지|취소|종료|소멸)"
    );
    private static final Pattern TRUST_MAINTENANCE = Pattern.compile(
            "(?<![가-힣A-Za-z0-9])신탁원부\\s*(?:변경|정정|경정)"
    );
    private static final String[] MORTGAGE_KEYWORDS = {
            "근저당", "채권최고액", "저당권"
    };
    private static final String[] TERMINATION_KEYWORDS = {
            "말소", "해제", "해지", "취소", "종료", "소멸", "취하"
    };
    private static final Map<String, String[]> INFRINGEMENT_CATEGORIES =
            infringementCategories();
    /** 등기 변동 이력 항목의 접수일자 후보 필드. 앞에 있는 것부터 먼저 본다. */
    private static final List<String> HISTORY_DATE_KEYS = List.of(
            "resReceiptDate", "resReceiptDe", "resReceiptDay",
            "resAcceptDate", "resRegistrationDate", "resContentsDate", "resDate"
    );
    private static final Set<String> DONG_IDENTITY_KEYS = Set.of(
            "resDong", "resDongNm", "resDongName", "commDongNum"
    );
    private static final Set<String> HO_IDENTITY_KEYS = Set.of(
            "resHo", "resHoNm", "resHoName", "commHoNum"
    );

    public RegistryData parse(Map<String, Object> data) {
        return parse(data, null);
    }

    public RegistryData parse(Map<String, Object> data, String detailAddress) {
        List<Map<String, Object>> entries = new ArrayList<>();
        collectMapsFromNamedList(data, "resRegisterEntriesList", entries);
        if (entries.isEmpty()) {
            return null;
        }

        RegistryRequestFactory.DongHo requested =
                RegistryRequestFactory.DongHo.parse(detailAddress);
        TargetValidation targetValidation = validateTargetIdentity(
                data,
                requested
        );
        if (!targetValidation.accepted()) {
            return null;
        }

        String summaryText = activeText(collectTextFromNamedNode(
                entries, "resRegistrationSumList"));
        String historyText = activeText(collectOrderedHistoryText(entries));
        String fallbackText = activeText(extractAllText(entries));
        boolean usesSummary = !summaryText.isBlank();
        boolean usesHistory = !usesSummary && !historyText.isBlank();
        String currentText = usesSummary
                ? summaryText : (usesHistory ? historyText : fallbackText);
        if (currentText.isBlank()) {
            return null;
        }

        OwnerExtraction owners = extractOwners(currentText, usesHistory);
        if (owners.names().isEmpty() && !historyText.isBlank() && !usesHistory) {
            owners = extractOwners(historyText, true);
        }
        String ownerType = inferOwnerType(owners);
        TrustHistoryState trustHistory = scanTrustHistory(historyText);
        boolean trustOwner = owners.hasTrusteeRole()
                || "TRUST_COMPANY".equals(ownerType);
        Boolean hasTrust = usesHistory
                ? (trustOwner ? true : trustHistory.currentValue())
                : trustOwner || containsCurrentTrust(currentText);
        Long mortgageAmount = usesHistory
                ? extractHistoricalMortgageAmount(historyText)
                : extractCurrentMortgageAmount(currentText);
        Boolean hasInfringement = usesHistory
                ? scanHistoricalInfringement(historyText)
                : containsCurrentInfringement(currentText);

        if (!hasSemanticEvidence(
                currentText, owners, mortgageAmount, hasInfringement, hasTrust)) {
            return null;
        }

        List<String> ownerNames = List.copyOf(owners.names());
        RegistryData result = new RegistryData();
        result.setOwnerNames(ownerNames);
        result.setOwnerName(ownerNames.isEmpty() ? null : String.join("|", ownerNames));
        result.setOwnerType(ownerType);
        result.setMortgageAmount(mortgageAmount);
        result.setHasSeizure(hasInfringement);
        result.setHasTrustRegistration(hasTrust);
        result.setRequestedDong(requested.dong());
        result.setRequestedHo(requested.ho());
        result.setTargetIdentityVerified(targetValidation.verified());
        result.setHasPostTrustInfringement(
                detectPostTrustInfringement(hasTrust, historyText));
        return result;
    }

    private TargetValidation validateTargetIdentity(
            Object data,
            RegistryRequestFactory.DongHo requested
    ) {
        if (requested.dong() == null && requested.ho() == null) {
            return new TargetValidation(true, null);
        }

        List<String> responseDongs = new ArrayList<>();
        List<String> responseHos = new ArrayList<>();
        collectValuesForKeys(data, DONG_IDENTITY_KEYS, responseDongs);
        collectValuesForKeys(data, HO_IDENTITY_KEYS, responseHos);
        DimensionValidation dong = validateDimension(
                requested.dong(), responseDongs, "동");
        DimensionValidation ho = validateDimension(
                requested.ho(), responseHos, "호");
        if (!dong.accepted() || !ho.accepted()) {
            return new TargetValidation(false, false);
        }
        boolean allAvailable = (requested.dong() == null || dong.available())
                && (requested.ho() == null || ho.available());
        return new TargetValidation(true, allAvailable ? true : null);
    }

    private DimensionValidation validateDimension(
            String expected,
            List<String> actualValues,
            String suffix
    ) {
        if (expected == null) {
            return new DimensionValidation(true, false);
        }
        List<String> normalizedActual = actualValues.stream()
                .map(value -> normalizeUnitToken(value, suffix))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (normalizedActual.isEmpty()) {
            return new DimensionValidation(true, false);
        }
        String normalizedExpected = normalizeUnitToken(expected, suffix);
        return new DimensionValidation(
                normalizedActual.contains(normalizedExpected),
                true
        );
    }

    private String normalizeUnitToken(String value, String suffix) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim()
                .replaceAll("\\s+", "")
                .replaceFirst(Pattern.quote(suffix) + "$", "")
                .toUpperCase(Locale.ROOT);
        return normalized.matches("\\d+")
                ? normalized.replaceFirst("^0+(?!$)", "")
                : normalized;
    }

    private void collectValuesForKeys(
            Object node,
            Set<String> keys,
            List<String> destination
    ) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (keys.contains(String.valueOf(entry.getKey()))
                        && entry.getValue() instanceof CharSequence value
                        && !value.toString().isBlank()) {
                    destination.add(value.toString());
                }
                collectValuesForKeys(entry.getValue(), keys, destination);
            }
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(item -> collectValuesForKeys(
                    item, keys, destination));
        }
    }

    private boolean hasSemanticEvidence(
            String text,
            OwnerExtraction owners,
            Long mortgageAmount,
            Boolean hasInfringement,
            Boolean hasTrust
    ) {
        if (!owners.names().isEmpty()
                || containsKeyword(text, MORTGAGE_KEYWORDS)
                || containsAnyInfringement(text)
                || containsTrustReference(text)) {
            return true;
        }
        return mortgageAmount == null
                || Boolean.TRUE.equals(hasInfringement)
                || Boolean.TRUE.equals(hasTrust);
    }

    private Boolean detectPostTrustInfringement(Boolean hasTrust, String historyText) {
        if (Boolean.FALSE.equals(hasTrust)) {
            return false;
        }
        if (hasTrust == null || historyText == null || historyText.isBlank()) {
            return null;
        }
        boolean boundarySeen = false;
        boolean trustActive = false;
        Map<String, Integer> rights = emptyRightCounts();
        for (String line : lines(historyText)) {
            if (isTrustTermination(line)) {
                boundarySeen = true;
                trustActive = false;
                rights.replaceAll((key, value) -> 0);
                continue;
            }
            if (isTrustEstablishment(line)) {
                boundarySeen = true;
                trustActive = true;
                rights.replaceAll((key, value) -> 0);
            }
            if (trustActive) {
                updateRightCounts(rights, line);
            }
        }
        if (!boundarySeen) {
            return null;
        }
        return rights.values().stream().anyMatch(count -> count > 0);
    }

    private Long extractCurrentMortgageAmount(String text) {
        String active = withoutTerminationLines(text, MORTGAGE_KEYWORDS);
        int markerCount = countMatches(MORTGAGE_MARKER, active);
        Matcher matcher = MORTGAGE_AMOUNT.matcher(active);
        int parsedCount = 0;
        long total = 0L;
        while (matcher.find()) {
            parsedCount++;
            try {
                total = Math.addExact(total, Long.parseLong(
                        matcher.group(1).replace(",", "")));
            } catch (ArithmeticException | NumberFormatException ignored) {
                return null;
            }
        }
        if (markerCount > 0) {
            return parsedCount == markerCount ? total : null;
        }
        return containsKeyword(active, MORTGAGE_KEYWORDS) ? null : 0L;
    }

    private Long extractHistoricalMortgageAmount(String historyText) {
        List<MortgageEntry> activeEntries = new ArrayList<>();
        for (String line : lines(historyText)) {
            if (!containsKeyword(line, MORTGAGE_KEYWORDS)) {
                continue;
            }
            if (isTermination(line)) {
                if (activeEntries.size() > 1) {
                    // 응답 텍스트에는 말소 대상 순위번호와 설정 항목의 구조적
                    // 결속이 없다. 임의로 마지막 항목을 지우면 잔액을 과소평가할
                    // 수 있으므로 다건 중 일부 말소는 미확인으로 처리한다.
                    return null;
                }
                if (activeEntries.size() == 1) {
                    activeEntries.remove(0);
                }
                continue;
            }
            if (line.contains("채권최고액")) {
                Long amount = parseAllMortgageMarkers(line);
                if (!activeEntries.isEmpty()
                        && activeEntries.get(activeEntries.size() - 1).amount == null) {
                    activeEntries.get(activeEntries.size() - 1).amount = amount;
                } else {
                    activeEntries.add(new MortgageEntry(amount));
                }
            } else {
                activeEntries.add(new MortgageEntry(null));
            }
        }
        if (activeEntries.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (MortgageEntry entry : activeEntries) {
            if (entry.amount == null) {
                return null;
            }
            try {
                total = Math.addExact(total, entry.amount);
            } catch (ArithmeticException ignored) {
                return null;
            }
        }
        return total;
    }

    private Long parseAllMortgageMarkers(String text) {
        int markerCount = countMatches(MORTGAGE_MARKER, text);
        Matcher matcher = MORTGAGE_AMOUNT.matcher(text);
        int parsedCount = 0;
        long total = 0L;
        while (matcher.find()) {
            parsedCount++;
            try {
                total = Math.addExact(total, Long.parseLong(
                        matcher.group(1).replace(",", "")));
            } catch (ArithmeticException | NumberFormatException ignored) {
                return null;
            }
        }
        return markerCount > 0 && parsedCount == markerCount ? total : null;
    }

    private OwnerExtraction extractOwners(String text, boolean latestOnly) {
        OwnerAccumulator accumulator = new OwnerAccumulator(latestOnly);
        Matcher prefix = OWNER_PREFIX.matcher(text);
        while (prefix.find()) {
            accumulator.add(prefix.group(1), cleanOwnerName(prefix.group(2)));
        }
        Matcher suffix = OWNER_SUFFIX_ROLE.matcher(text);
        while (suffix.find()) {
            accumulator.add(suffix.group(2), cleanOwnerName(suffix.group(1)));
        }
        return accumulator.result();
    }

    private String cleanOwnerName(String source) {
        if (source == null) {
            return null;
        }
        String candidate = source.trim();
        candidate = ID_NUMBER.matcher(candidate).replaceFirst("");
        candidate = OWNER_TRAILING_FIELDS.matcher(candidate).replaceFirst("");
        candidate = candidate.replaceFirst("^(?:\\d+\\s+)?소유권이전\\s+", "");
        candidate = candidate.replaceAll("\\s{2,}", " ").trim();
        return candidate.isBlank() ? null : candidate;
    }

    private String inferOwnerType(OwnerExtraction owners) {
        if (owners.names().isEmpty()) {
            return null;
        }
        if (owners.hasTrusteeRole()
                || owners.names().stream().anyMatch(name -> name.contains("신탁"))) {
            return "TRUST_COMPANY";
        }
        if (owners.names().stream().anyMatch(this::isCorporationName)) {
            return "CORPORATION";
        }
        return "INDIVIDUAL";
    }

    private boolean isCorporationName(String ownerName) {
        return ownerName.contains("주식회사")
                || ownerName.contains("(주)")
                || ownerName.contains("법인")
                || ownerName.endsWith("은행");
    }

    private boolean containsCurrentInfringement(String text) {
        for (String line : lines(text)) {
            if (!isTermination(line) && containsAnyInfringement(line)) {
                return true;
            }
        }
        return false;
    }

    private boolean scanHistoricalInfringement(String text) {
        Map<String, Integer> counts = emptyRightCounts();
        for (String line : lines(text)) {
            updateRightCounts(counts, line);
        }
        return counts.values().stream().anyMatch(count -> count > 0);
    }

    private void updateRightCounts(Map<String, Integer> counts, String line) {
        for (Map.Entry<String, String[]> category : INFRINGEMENT_CATEGORIES.entrySet()) {
            if (!containsKeyword(line, category.getValue())) {
                continue;
            }
            // '가압류'에는 '압류'가 포함되지만 별개의 권리다. 두 건으로
            // 세면 다른 압류의 말소까지 함께 차감될 수 있다.
            if ("SEIZURE".equals(category.getKey()) && line.contains("가압류")) {
                continue;
            }
            int current = counts.get(category.getKey());
            counts.put(category.getKey(), isTermination(line)
                    ? Math.max(0, current - 1) : current + 1);
        }
    }

    private Map<String, Integer> emptyRightCounts() {
        Map<String, Integer> result = new LinkedHashMap<>();
        INFRINGEMENT_CATEGORIES.keySet().forEach(key -> result.put(key, 0));
        return result;
    }

    private boolean containsAnyInfringement(String text) {
        return INFRINGEMENT_CATEGORIES.values().stream()
                .anyMatch(keywords -> containsKeyword(text, keywords));
    }

    private boolean containsCurrentTrust(String text) {
        for (String line : lines(text)) {
            if (!isTrustTermination(line) && containsTrustEvent(line)) {
                return true;
            }
        }
        return false;
    }

    private TrustHistoryState scanTrustHistory(String text) {
        boolean boundarySeen = false;
        boolean active = false;
        boolean maintenanceWithoutBoundary = false;
        for (String line : lines(text)) {
            if (isTrustTermination(line)) {
                boundarySeen = true;
                active = false;
            } else if (isTrustEstablishment(line)) {
                boundarySeen = true;
                active = true;
            } else if (isTrustMaintenance(line)) {
                maintenanceWithoutBoundary = maintenanceWithoutBoundary || !boundarySeen;
            }
        }
        return new TrustHistoryState(boundarySeen, active, maintenanceWithoutBoundary);
    }

    private boolean containsTrustReference(String text) {
        for (String line : lines(text)) {
            if (containsTrustEvent(line)
                    || isTrustTermination(line)
                    || isTrustMaintenance(line)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTrustEstablishment(String line) {
        return !isTrustTermination(line)
                && !isTrustMaintenance(line)
                && containsTrustEvent(line);
    }

    private boolean containsTrustEvent(String line) {
        boolean explicit = TRUST_EXPLICIT.matcher(line).find();
        boolean standalone = TRUST_STANDALONE.matcher(line).find();
        if (!explicit && !standalone) {
            return false;
        }
        boolean nonOwnerParty = containsKeyword(
                line, "근저당권자", "저당권자", "채권자", "전세권자", "임차권자");
        return !nonOwnerParty || explicit;
    }

    private boolean isTrustTermination(String line) {
        return TRUST_TERMINATION.matcher(line).find();
    }

    private boolean isTrustMaintenance(String line) {
        return TRUST_MAINTENANCE.matcher(line).find()
                && !isTrustTermination(line);
    }

    private boolean isTermination(String line) {
        return containsKeyword(line, TERMINATION_KEYWORDS);
    }

    private String withoutTerminationLines(String text, String... subjectKeywords) {
        StringBuilder result = new StringBuilder();
        for (String line : lines(text)) {
            if (isTermination(line) && containsKeyword(line, subjectKeywords)) {
                continue;
            }
            result.append(line).append('\n');
        }
        return result.toString();
    }

    private List<String> lines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String normalized = line.trim();
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private int countMatches(Pattern pattern, String text) {
        int count = 0;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private boolean containsKeyword(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String activeText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return CANCELLED_SPAN.matcher(text).replaceAll("");
    }

    /**
     * 등기 변동 이력을 접수일자 순으로 이어 붙인다.
     *
     * 응답은 갑구 이력 전체 뒤에 을구 이력 전체가 오는 형태라, 받은 순서대로
     * 읽으면 시간 순서가 아니다. 그대로 스캔하면 신탁등기보다 먼저 설정된
     * 을구 권리가 "신탁 이후에 붙은 권리"로 잡혀 3-C가 오판한다.
     *
     * 모든 항목에서 접수일자를 읽어낸 경우에만 정렬한다. 하나라도 읽지 못하면
     * 잘못 섞을 위험이 있으므로 응답이 준 순서를 그대로 둔다.
     */
    private String collectOrderedHistoryText(Object node) {
        List<Map<String, Object>> items = new ArrayList<>();
        collectMapsFromNamedList(node, "resRegistrationHisList", items);
        if (items.isEmpty()) {
            return collectTextFromNamedNode(node, "resRegistrationHisList");
        }

        List<HistoryEntry> ordered = new ArrayList<>(items.size());
        boolean everyEntryDated = true;
        for (int index = 0; index < items.size(); index++) {
            String date = findHistoryDate(items.get(index));
            if (date == null) {
                everyEntryDated = false;
            }
            ordered.add(new HistoryEntry(index, date, items.get(index)));
        }
        if (everyEntryDated) {
            ordered.sort(
                    Comparator.comparing(HistoryEntry::date)
                            .thenComparingInt(HistoryEntry::index)
            );
        }

        StringBuilder destination = new StringBuilder();
        for (HistoryEntry entry : ordered) {
            appendTextValues(entry.data(), destination);
        }
        return destination.toString();
    }

    private String findHistoryDate(Map<String, Object> entry) {
        for (String key : HISTORY_DATE_KEYS) {
            String date = normalizeHistoryDate(entry.get(key));
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    /** "2020.05.13 제12345호" 같은 값에서 앞 8자리 날짜만 뽑아 비교 가능하게 만든다. */
    private String normalizeHistoryDate(Object raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.toString().replaceAll("[^0-9]", "");
        if (digits.length() < 8) {
            return null;
        }
        String date = digits.substring(0, 8);
        return date.matches("(?:19|20)[0-9]{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12][0-9]|3[01])")
                ? date
                : null;
    }

    private String collectTextFromNamedNode(Object node, String targetKey) {
        StringBuilder destination = new StringBuilder();
        collectTextFromNamedNode(node, targetKey, destination);
        return destination.toString();
    }

    private void collectTextFromNamedNode(
            Object node, String targetKey, StringBuilder destination) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (targetKey.equals(String.valueOf(entry.getKey()))) {
                    appendTextValues(entry.getValue(), destination);
                } else {
                    collectTextFromNamedNode(entry.getValue(), targetKey, destination);
                }
            }
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(item -> collectTextFromNamedNode(
                    item, targetKey, destination));
        }
    }

    private String extractAllText(Object node) {
        StringBuilder destination = new StringBuilder();
        appendTextValues(node, destination);
        return destination.toString();
    }

    private void appendTextValues(Object node, StringBuilder destination) {
        if (node instanceof Map<?, ?> map) {
            map.values().forEach(value -> appendTextValues(value, destination));
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(value -> appendTextValues(value, destination));
        } else if (node instanceof CharSequence text
                && !text.toString().isBlank()) {
            destination.append(text).append('\n');
        }
    }

    @SuppressWarnings("unchecked")
    private void collectMapsFromNamedList(
            Object node,
            String targetKey,
            List<Map<String, Object>> destination
    ) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (targetKey.equals(String.valueOf(entry.getKey()))
                        && value instanceof Collection<?> collection) {
                    for (Object item : collection) {
                        if (item instanceof Map<?, ?> itemMap) {
                            destination.add((Map<String, Object>) itemMap);
                        }
                    }
                } else {
                    collectMapsFromNamedList(value, targetKey, destination);
                }
            }
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(item -> collectMapsFromNamedList(
                    item, targetKey, destination));
        }
    }

    private static Map<String, String[]> infringementCategories() {
        Map<String, String[]> result = new LinkedHashMap<>();
        result.put("PROVISIONAL_SEIZURE", new String[]{"가압류"});
        result.put("SEIZURE", new String[]{"압류"});
        result.put("DISPOSITION", new String[]{"가처분", "처분금지가처분"});
        result.put("PROVISIONAL_REGISTRATION", new String[]{"가등기", "담보가등기"});
        result.put("AUCTION", new String[]{"경매개시결정", "강제경매", "임의경매", "공매"});
        // 전세권은 여기에 넣지 않는다. 선순위 전세권은 보증금 계산에서 따져야 할
        // 선순위 권리일 뿐 압류·경매 같은 권리침해가 아니다. 함께 세면 앞 세입자의
        // 전세권등기가 남아 있는 정상 매물까지 권리침해 DANGER가 되고, HUG 판정도
        // 연쇄로 DANGER가 된다.
        // 임차권등기는 다르다. 보증금을 돌려받지 못해 법원 명령으로 붙는 등기라
        // 그 자체가 확실한 위험 신호다.
        result.put("LEASE_RIGHT", new String[]{"임차권등기"});
        return result;
    }

    private static final class MortgageEntry {
        private Long amount;

        private MortgageEntry(Long amount) {
            this.amount = amount;
        }
    }

    private static final class OwnerAccumulator {
        private final boolean latestOnly;
        private final Set<String> names = new TreeSet<>();
        private boolean hasTrusteeRole;

        private OwnerAccumulator(boolean latestOnly) {
            this.latestOnly = latestOnly;
        }

        private void add(String role, String name) {
            if (name == null) {
                return;
            }
            if (latestOnly && ("소유자".equals(role) || "수탁자".equals(role))) {
                names.clear();
                hasTrusteeRole = false;
            }
            names.add(name);
            hasTrusteeRole = hasTrusteeRole || "수탁자".equals(role);
        }

        private OwnerExtraction result() {
            return new OwnerExtraction(names, hasTrusteeRole);
        }
    }

    private record OwnerExtraction(Set<String> names, boolean hasTrusteeRole) {
    }

    private record HistoryEntry(int index, String date, Map<String, Object> data) {
    }

    private record DimensionValidation(boolean accepted, boolean available) {
    }

    private record TargetValidation(boolean accepted, Boolean verified) {
    }

    private record TrustHistoryState(
            boolean boundarySeen,
            boolean active,
            boolean maintenanceWithoutBoundary
    ) {
        private Boolean currentValue() {
            if (boundarySeen) {
                return active;
            }
            return maintenanceWithoutBoundary ? null : false;
        }
    }
}
