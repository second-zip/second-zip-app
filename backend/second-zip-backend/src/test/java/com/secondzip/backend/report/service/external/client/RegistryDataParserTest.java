package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.external.RegistryData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryDataParserTest {

    private final RegistryDataParser parser = new RegistryDataParser();

    @Test
    void usesSummaryForCurrentRightsAndIgnoresCancelledText() {
        RegistryData result = parser.parse(data(
                "소유자 주식회사 세컨드집 법인등록번호 110111-1234567\n"
                        + "신탁\n"
                        + "채권최고액 금 100,000,000원\n"
                        + "&채권최고액 금 900,000,000원 가압류&",
                "소유권이전\n신탁\n가압류"
        ));

        assertEquals(100_000_000L, result.getMortgageAmount());
        assertFalse(result.getHasSeizure());
        assertTrue(result.getHasTrustRegistration());
        assertEquals("주식회사 세컨드집", result.getOwnerName());
        assertEquals("CORPORATION", result.getOwnerType());
        assertTrue(result.getHasPostTrustInfringement());
    }

    @Test
    void noTrustProducesVerifiedFalsePostTrustResult() {
        RegistryData result = parser.parse(data(
                "소유자 홍길동 주민등록번호 900101-*******\n압류",
                "소유권이전\n압류"
        ));

        assertEquals("홍길동", result.getOwnerName());
        assertEquals("INDIVIDUAL", result.getOwnerType());
        assertTrue(result.getHasSeizure());
        assertFalse(result.getHasTrustRegistration());
        assertFalse(result.getHasPostTrustInfringement());
    }

    @Test
    void infringementBeforeTrustIsNotPostTrustInfringement() {
        RegistryData result = parser.parse(data(
                "소유자 한국자산신탁\n신탁",
                "가압류\n신탁"
        ));

        assertEquals("TRUST_COMPANY", result.getOwnerType());
        assertFalse(result.getHasPostTrustInfringement());
    }

    private Map<String, Object> data(
            String summaryText,
            String historyText
    ) {
        return Map.of(
                "resRegisterEntriesList",
                List.of(Map.of(
                        "resRegistrationSumList",
                        List.of(contents(summaryText)),
                        "resRegistrationHisList",
                        List.of(contents(historyText))
                ))
        );
    }

    private Map<String, Object> contents(String text) {
        return Map.of(
                "resContentsList",
                List.of(Map.of(
                        "resDetailList",
                        List.of(Map.of("resContents", text))
                ))
        );
    }
}
