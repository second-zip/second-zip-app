package com.secondzip.backend.report.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.FraudType;
import com.secondzip.backend.report.enums.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 프론트와 합의한 응답 계약을 고정한다.
 *
 * <p>외부 API를 부르지 않고 직렬화 결과만 검사하므로 <b>비용이 들지 않는다.</b>
 * 프론트에 스펙을 넘기기 전에 이 테스트로 실제 JSON 모양을 확인할 수 있다.
 *
 * <p>여기가 깨지면 프론트 화면도 같이 깨진다는 뜻이다.
 * 문서(프론트-전달-백엔드-변경사항.md)와 함께 수정해야 한다.
 */
class ReportDetailResponseContractTest {

    // 운영과 동일한 설정(별도 커스터마이징 없음)
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("checkResults와 detailResults에 dataStatus가 포함된다")
    void exposesDataStatusOnEveryJudgement() throws Exception {
        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(sample())
        );

        JsonNode check = json.get("checkResults").get(0);
        assertTrue(check.has("dataStatus"), "checkResults에 dataStatus가 있어야 한다");
        assertEquals("UNVERIFIED", check.get("dataStatus").asText());

        JsonNode detail = json.get("fraudTypes").get(0)
                .get("detailResults").get(0);
        assertTrue(detail.has("dataStatus"), "detailResults에 dataStatus가 있어야 한다");
        assertEquals("NOT_APPLICABLE", detail.get("dataStatus").asText());
    }

    @Test
    @DisplayName("확인하지 못한 evidence 값은 키가 사라지지 않고 null로 내려간다")
    void keepsUnverifiedEvidenceAsExplicitNull() throws Exception {
        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(sample())
        );

        JsonNode evidence = json.get("checkResults").get(0).get("evidence");

        assertTrue(
                evidence.has("mortgageAmount"),
                "키 자체가 빠지면 프론트가 'undefined'와 '확인 불가'를 구분할 수 없다"
        );
        assertTrue(
                evidence.get("mortgageAmount").isNull(),
                "확인하지 못한 값은 0이 아니라 null이어야 한다"
        );
    }

    @Test
    @DisplayName("건축물 유형과 신탁 여부가 응답에 포함된다")
    void exposesHousingCategoryAndTrustProperty() throws Exception {
        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(sample())
        );

        assertTrue(json.has("housingCategory"), "housingCategory가 있어야 한다");
        assertEquals("OFFICETEL", json.get("housingCategory").asText());

        assertTrue(json.has("trustProperty"), "trustProperty가 있어야 한다");
        assertTrue(json.get("trustProperty").asBoolean());
    }

    @Test
    @DisplayName("이 필드 추가 전에 만들어진 리포트는 두 값이 null로 내려간다")
    void keepsLegacyReportsAsExplicitNull() throws Exception {
        ReportDetailResponse legacy = new ReportDetailResponse(
                2L,
                "서울 강남구 테헤란로 152",
                null,
                500_000_000L,
                RiskLevel.CAUTION,
                false,
                null,
                null,
                List.of(),
                List.of(),
                List.of()
        );

        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(legacy)
        );

        // 키가 사라지면 프론트가 '없음'과 '아직 안 옴'을 구분할 수 없다
        assertTrue(json.has("housingCategory"));
        assertTrue(json.get("housingCategory").isNull());
        assertTrue(json.has("trustProperty"));
        assertTrue(json.get("trustProperty").isNull());
    }

    @Test
    @DisplayName("위험도 enum은 SAFE/CAUTION/DANGER 3개를 유지한다")
    void riskLevelEnumIsUnchanged() {
        assertEquals(3, RiskLevel.values().length);
        assertEquals(
                List.of("SAFE", "CAUTION", "DANGER"),
                List.of(
                        RiskLevel.SAFE.name(),
                        RiskLevel.CAUTION.name(),
                        RiskLevel.DANGER.name()
                )
        );
    }

    /** 실패한 외부 조회가 섞인, 프론트가 실제로 마주할 수 있는 응답. */
    private ReportDetailResponse sample() {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("mortgageAmount", null); // 등기 파싱 실패

        return new ReportDetailResponse(
                1L,
                "서울 강남구 테헤란로 152",
                "101동 1203호",
                500_000_000L,
                RiskLevel.CAUTION,
                false,
                "OFFICETEL",
                true,
                List.of(new CheckResultView(
                        CheckType.MORTGAGE_EXISTENCE,
                        RiskLevel.CAUTION,
                        DataStatus.UNVERIFIED,
                        evidence
                )),
                List.of(new FraudTypeView(
                        FraudType.FALSE_INFORMATION_RIGHTS_CONCEALMENT,
                        RiskLevel.CAUTION,
                        List.of(new DetailResultView(
                                DetailType.LAND_BUILDING_OWNERSHIP_MISMATCH,
                                RiskLevel.CAUTION,
                                DataStatus.NOT_APPLICABLE
                        ))
                )),
                List.of()
        );
    }
}
