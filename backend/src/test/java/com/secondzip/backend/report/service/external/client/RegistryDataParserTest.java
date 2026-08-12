package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.external.RegistryData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void trustCompanyOwnerImpliesTrustRegistration() {
        // 요약본에는 "신탁" 문구가 없고, 소유자는 변동 이력에서만 확인된다.
        RegistryData result = parser.parse(data(
                "채권최고액 금 100,000,000원",
                "소유권이전 소유자 한국자산신탁 주식회사"
        ));

        assertTrue(
                result.getHasTrustRegistration(),
                "소유권이 수탁자에게 있으면 신탁등기가 존재한다는 뜻이다. "
                        + "여기서 놓치면 신탁 체크리스트가 통째로 누락된다."
        );
        assertEquals("TRUST_COMPANY", result.getOwnerType());
    }

    @Test
    void individualOwnerWithoutTrustTextIsNotTrust() {
        RegistryData result = parser.parse(data(
                "채권최고액 금 100,000,000원",
                "소유권이전 소유자 홍길동"
        ));

        assertFalse(result.getHasTrustRegistration());
    }

    @Test
    void mortgageKeywordWithoutReadableAmountIsUnknown() {
        RegistryData result = parser.parse(data(
                "소유자 홍길동\n근저당권설정 채권최고액 금 (판독불가) 원",
                "근저당권설정"
        ));

        assertNull(
                result.getMortgageAmount(),
                "금액을 읽지 못했으면 0이 아니라 null이어야 한다. "
                        + "0으로 내보내면 상위 판정이 '근저당 없음 = 안전'으로 오판한다."
        );
    }

    @Test
    void absenceOfMortgageKeywordMeansNoMortgage() {
        RegistryData result = parser.parse(data(
                "소유자 홍길동 주민등록번호 900101-*******",
                "소유권이전"
        ));

        assertEquals(0L, result.getMortgageAmount());
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
