package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.external.RegistryData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RegistryDataParser {

    private static final Pattern CANCELLED_SPAN =
            Pattern.compile("&[^&]*&", Pattern.DOTALL);
    private static final Pattern MORTGAGE_AMOUNT =
            Pattern.compile("채권최고액\\s*금?\\s*([0-9,]+)\\s*원?");
    private static final Pattern OWNER =
            Pattern.compile("(?:소유자|공유자)\\s*[:：]?\\s*([^\\n|]+)");
    private static final Pattern OWNER_SUFFIX =
            Pattern.compile(
                    "\\s+(?:주민등록번호|법인등록번호|주소|지분|순위번호|등기원인"
                            + "|채권최고액|근저당권|압류|가압류|신탁|경매).*$"
            );
    private static final Pattern ID_NUMBER =
            Pattern.compile("\\s+\\d{6}[-*]\\d{7}.*$");
    private static final String[] INFRINGEMENT_KEYWORDS = {
            "압류", "가압류", "경매개시결정", "강제경매", "임의경매"
    };
    /** 근저당 관련 표현. 금액 파싱 실패와 "근저당 없음"을 구분하는 데 쓴다. */
    private static final String[] MORTGAGE_KEYWORDS = {
            "근저당", "채권최고액", "저당권"
    };

    public RegistryData parse(Map<String, Object> data) {
        List<Map<String, Object>> entries = new ArrayList<>();
        collectMapsFromNamedList(data, "resRegisterEntriesList", entries);
        if (entries.isEmpty()) {
            return null;
        }

        String summaryText = activeText(
                collectTextFromNamedNode(
                        entries,
                        "resRegistrationSumList"
                )
        );
        String historyText = activeText(
                collectTextFromNamedNode(
                        entries,
                        "resRegistrationHisList"
                )
        );
        String fallbackText = activeText(extractAllText(entries));
        String currentText = !summaryText.isBlank()
                ? summaryText
                : (!historyText.isBlank() ? historyText : fallbackText);
        if (currentText.isBlank()) {
            return null;
        }

        RegistryData result = new RegistryData();
        result.setMortgageAmount(extractMortgageAmount(currentText));
        result.setHasSeizure(
                containsKeyword(currentText, INFRINGEMENT_KEYWORDS)
        );
        // 소유자를 먼저 뽑는다. 신탁 판정에 소유자 유형이 필요하기 때문이다.
        String ownerName = extractOwnerName(currentText);
        if (ownerName == null && !historyText.isBlank()) {
            ownerName = extractOwnerName(historyText);
        }
        String ownerType = inferOwnerType(ownerName);
        result.setOwnerName(ownerName);
        result.setOwnerType(ownerType);

        boolean hasTrust = detectTrust(currentText, ownerType);
        result.setHasTrustRegistration(hasTrust);
        result.setHasPostTrustInfringement(
                detectPostTrustInfringement(hasTrust, historyText)
        );
        return result;
    }

    /**
     * 신탁등기 존재 여부.
     *
     * 두 경로로 판정.
     * 현재 권리관계 텍스트에 "신탁" 문구가 있다
     * 소유자가 신탁회사다 : 소유권이 수탁자에게 넘어가 있다는 뜻이므로
     *       신탁등기가 존재한다고 봄.
     */
    private boolean detectTrust(String currentText, String ownerType) {
        return containsKeyword(currentText, "신탁")
                || "TRUST_COMPANY".equals(ownerType);
    }

    private Boolean detectPostTrustInfringement(
            boolean hasTrust,
            String historyText
    ) {
        if (!hasTrust) {
            return false;
        }
        if (historyText == null || historyText.isBlank()) {
            return null;
        }
        int trustIndex = historyText.lastIndexOf("신탁");
        if (trustIndex < 0) {
            return null;
        }
        return containsKeyword(
                historyText.substring(trustIndex + "신탁".length()),
                INFRINGEMENT_KEYWORDS
        );
    }

    /**
     * 채권최고액 합계.
     *
     * "근저당 없음"과 "금액을 못 읽음"을 반드시 구분.
     *
     *   금액을 하나라도 읽었다 → 합계
     *   금액은 못 읽었는데 근저당 관련 표현은 있다 → null (확인 불가)
     *   근저당 관련 표현 자체가 없다 → 0 (근저당 없음)
     *
     * 확인 불가를 0으로 내보내면 근저당이 잡혀 있는 매물이
     * "근저당 없음 = 안전"으로 판정된다. 등기부 표기가 조금만 달라도 발생하므로
     * 여기서 반드시 null로 흘려보내 상위 판정이 CAUTION을 주도록 한다.
     */
    private Long extractMortgageAmount(String text) {
        long total = 0L;
        boolean amountParsed = false;
        Matcher matcher = MORTGAGE_AMOUNT.matcher(text);
        while (matcher.find()) {
            try {
                total += Long.parseLong(
                        matcher.group(1).replace(",", "")
                );
                amountParsed = true;
            } catch (NumberFormatException ignored) {
            }
        }
        if (amountParsed) {
            return total;
        }
        return containsKeyword(text, MORTGAGE_KEYWORDS) ? null : 0L;
    }

    private String extractOwnerName(String text) {
        Matcher matcher = OWNER.matcher(text);
        String lastOwner = null;
        while (matcher.find()) {
            String candidate = matcher.group(1).trim();
            candidate = OWNER_SUFFIX.matcher(candidate).replaceFirst("");
            candidate = ID_NUMBER.matcher(candidate).replaceFirst("");
            candidate = candidate.replaceAll("\\s{2,}", " ").trim();
            if (!candidate.isBlank()) {
                lastOwner = candidate;
            }
        }
        return lastOwner;
    }

    private String inferOwnerType(String ownerName) {
        if (ownerName == null) {
            return null;
        }
        if (ownerName.contains("신탁")) {
            return "TRUST_COMPANY";
        }
        if (ownerName.contains("주식회사")
                || ownerName.contains("(주)")
                || ownerName.contains("법인")
                || ownerName.endsWith("은행")) {
            return "CORPORATION";
        }
        return "INDIVIDUAL";
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

    private String collectTextFromNamedNode(
            Object node,
            String targetKey
    ) {
        StringBuilder destination = new StringBuilder();
        collectTextFromNamedNode(node, targetKey, destination);
        return destination.toString();
    }

    private void collectTextFromNamedNode(
            Object node,
            String targetKey,
            StringBuilder destination
    ) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (targetKey.equals(String.valueOf(entry.getKey()))) {
                    appendTextValues(entry.getValue(), destination);
                } else {
                    collectTextFromNamedNode(
                            entry.getValue(),
                            targetKey,
                            destination
                    );
                }
            }
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(
                    item -> collectTextFromNamedNode(
                            item,
                            targetKey,
                            destination
                    )
            );
        }
    }

    private String extractAllText(Object node) {
        StringBuilder destination = new StringBuilder();
        appendTextValues(node, destination);
        return destination.toString();
    }

    private void appendTextValues(Object node, StringBuilder destination) {
        if (node instanceof Map<?, ?> map) {
            map.values().forEach(
                    value -> appendTextValues(value, destination)
            );
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(
                    value -> appendTextValues(value, destination)
            );
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
                            destination.add(
                                    (Map<String, Object>) itemMap
                            );
                        }
                    }
                } else {
                    collectMapsFromNamedList(
                            value,
                            targetKey,
                            destination
                    );
                }
            }
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(
                    item -> collectMapsFromNamedList(
                            item,
                            targetKey,
                            destination
                    )
            );
        }
    }
}
