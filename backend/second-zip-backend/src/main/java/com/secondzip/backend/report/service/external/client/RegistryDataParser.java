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
        boolean hasTrust = containsKeyword(currentText, "신탁");
        result.setHasTrustRegistration(hasTrust);

        String ownerName = extractOwnerName(currentText);
        if (ownerName == null && !historyText.isBlank()) {
            ownerName = extractOwnerName(historyText);
        }
        result.setOwnerName(ownerName);
        result.setOwnerType(inferOwnerType(ownerName));
        result.setHasPostTrustInfringement(
                detectPostTrustInfringement(hasTrust, historyText)
        );
        return result;
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

    private Long extractMortgageAmount(String text) {
        long total = 0L;
        Matcher matcher = MORTGAGE_AMOUNT.matcher(text);
        while (matcher.find()) {
            try {
                total += Long.parseLong(
                        matcher.group(1).replace(",", "")
                );
            } catch (NumberFormatException ignored) {
            }
        }
        return total;
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
