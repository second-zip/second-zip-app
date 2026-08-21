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

    @Test
    void removesMaskedResidentNumberFromOwnerName() {
        RegistryData result = parser.parse(data(
                "소유자 홍길동 700101-1******",
                ""
        ));

        assertEquals("홍길동", result.getOwnerName());
        assertEquals(List.of("홍길동"), result.getOwnerNames());
    }

    @Test
    void parsesSuffixOwnerOrderAndTrusteeRole() {
        RegistryData suffixOwner = parser.parse(data("홍길동 (소유자)", ""));
        RegistryData trustee = parser.parse(data(
                "수탁자 주식회사한국자산신탁",
                ""
        ));

        assertEquals("홍길동", suffixOwner.getOwnerName());
        assertEquals("주식회사한국자산신탁", trustee.getOwnerName());
        assertEquals("TRUST_COMPANY", trustee.getOwnerType());
        assertTrue(trustee.getHasTrustRegistration());
    }

    @Test
    void mortgageHolderCompanyNameDoesNotCreateTrustRegistration() {
        RegistryData result = parser.parse(data(
                "소유자 홍길동\n"
                        + "근저당권자 한국자산신탁 주식회사\n"
                        + "채권최고액 금 100,000,000원",
                ""
        ));

        assertFalse(result.getHasTrustRegistration());
        assertEquals("홍길동", result.getOwnerName());
    }

    @Test
    void trustLedgerChangeDoesNotMovePostTrustBoundary() {
        RegistryData result = parser.parse(data(
                "수탁자 한국자산신탁 주식회사\n신탁등기\n가압류",
                "신탁등기\n가압류\n신탁원부 변경"
        ));

        assertTrue(result.getHasPostTrustInfringement());
    }

    @Test
    void jeonseRightAloneIsNotTreatedAsRightsInfringement() {
        RegistryData result = parser.parse(data(
                "소유자 홍길동\n전세권설정 전세금 금 300,000,000원", ""
        ));

        assertFalse(
                result.getHasSeizure(),
                "앞 세입자의 전세권은 선순위 권리일 뿐 압류·경매 같은 권리침해가 아니다"
        );
    }

    @Test
    void leaseRightRegistrationIsStillRightsInfringement() {
        RegistryData result = parser.parse(data(
                "소유자 홍길동\n주택임차권등기", ""
        ));

        assertTrue(
                result.getHasSeizure(),
                "임차권등기는 보증금을 돌려받지 못해 법원 명령으로 붙는 등기다"
        );
    }

    @Test
    void ordersHistoryByReceiptDateBeforeScanningTrustBoundary() {
        // 응답은 갑구(신탁) 전체 뒤에 을구(가압류)가 오지만, 접수일자로는
        // 가압류가 신탁보다 앞선다. 받은 순서대로 읽으면 신탁 이후로 오인한다.
        RegistryData result = parser.parse(datedHistory(
                "20240301", "신탁등기",
                "20230101", "가압류"
        ));

        assertFalse(
                Boolean.TRUE.equals(result.getHasPostTrustInfringement()),
                "신탁보다 먼저 접수된 권리를 신탁 이후 권리로 세면 안 된다"
        );
    }

    @Test
    void expandedInfringementRightsAreDetected() {
        assertTrue(parser.parse(data(
                "소유자 홍길동\n처분금지가처분", ""
        )).getHasSeizure());
        assertTrue(parser.parse(data(
                "소유자 홍길동\n소유권이전청구권가등기", ""
        )).getHasSeizure());
    }

    @Test
    void oneUnreadableMortgageMakesWholeTotalUnknown() {
        RegistryData result = parser.parse(data(
                "소유자 홍길동\n"
                        + "채권최고액 금 100,000,000원\n"
                        + "채권최고액 금 (판독불가) 원",
                ""
        ));

        assertNull(result.getMortgageAmount());
    }

    @Test
    void coOwnersAreCanonicalizedAsAnOrderIndependentSet() {
        RegistryData first = parser.parse(data(
                "공유자 홍길동\n공유자 김철수", ""
        ));
        RegistryData second = parser.parse(data(
                "공유자 김철수\n공유자 홍길동", ""
        ));

        assertEquals(List.of("김철수", "홍길동"), first.getOwnerNames());
        assertEquals(first.getOwnerName(), second.getOwnerName());
    }

    @Test
    void historyOnlyAppliesMortgageSeizureAndTrustCancellations() {
        RegistryData result = parser.parse(data(
                "",
                "소유자 홍길동\n"
                        + "근저당권설정 채권최고액 금 100,000,000원\n"
                        + "근저당권말소\n"
                        + "가압류\n가압류말소\n"
                        + "신탁등기\n신탁등기말소"
        ));

        assertEquals(0L, result.getMortgageAmount());
        assertFalse(result.getHasSeizure());
        assertFalse(result.getHasTrustRegistration());
        assertFalse(result.getHasPostTrustInfringement());
    }

    @Test
    void historyWithMultipleMortgagesAndAmbiguousCancellationIsUnknown() {
        RegistryData result = parser.parse(data(
                "",
                "소유자 홍길동\n"
                        + "근저당권설정 A 채권최고액 금 100,000,000원\n"
                        + "근저당권설정 B 채권최고액 금 300,000,000원\n"
                        + "A번 근저당권말소"
        ));

        assertNull(
                result.getMortgageAmount(),
                "말소 항목과 설정 항목을 구조적으로 결속할 수 없으면 임의 차감해 과소평가하면 안 된다"
        );
    }

    @Test
    void headerOnlyResponseIsRejected() {
        assertNull(parser.parse(data(
                "등기사항전부증명서(열람용)\n갑구 소유권에 관한 사항",
                ""
        )));
    }

    @Test
    void validatesUnitIdentityWhenResponseExposesDongAndHo() {
        Map<String, Object> matching = Map.of(
                "resDong", "가동",
                "resHo", "B101호",
                "payload", data("소유자 홍길동", "")
        );
        Map<String, Object> differentUnit = Map.of(
                "resDong", "가동",
                "resHo", "B102호",
                "payload", data("소유자 홍길동", "")
        );

        RegistryData result = parser.parse(matching, "가동 B101호");

        assertEquals("가", result.getRequestedDong());
        assertEquals("B101", result.getRequestedHo());
        assertTrue(result.getTargetIdentityVerified());
        assertNull(parser.parse(differentUnit, "가동 B101호"));
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

    /**
     * 접수일자를 가진 변동 이력 두 건. 인자에 준 순서대로 응답에 담긴다.
     * 요약(갑구 현재 권리관계)에는 신탁만 남겨 이력 기반 판정을 타게 한다.
     */
    private Map<String, Object> datedHistory(
            String firstDate,
            String firstText,
            String secondDate,
            String secondText
    ) {
        return Map.of(
                "resRegisterEntriesList",
                List.of(Map.of(
                        "resRegistrationSumList",
                        List.of(contents("수탁자 한국자산신탁 주식회사")),
                        "resRegistrationHisList",
                        List.of(
                                historyEntry(firstDate, firstText),
                                historyEntry(secondDate, secondText)
                        )
                ))
        );
    }

    private Map<String, Object> historyEntry(String receiptDate, String text) {
        return Map.of(
                "resReceiptDate", receiptDate,
                "resContentsList",
                List.of(Map.of(
                        "resDetailList",
                        List.of(Map.of("resContents", text))
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
