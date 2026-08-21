package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AnalysisTargetDTO;
import com.secondzip.backend.report.dto.external.BuildingData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BuildingHubClientContractTest {

    private static final String EXPECTED_URL =
            "https://apis.data.go.kr/1613000/BldRgstHubService/getBrTitleInfo"
                    + "?serviceKey=test-key&sigunguCd=11680&bjdongCd=10100"
                    + "&bun=0737&ji=0084&numOfRows=100&pageNo=1&_type=json";

    private MockRestServiceServer server;
    private BuildingHubClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new BuildingHubClient(restTemplate);
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
    }

    @AfterEach
    void verifyServer() {
        server.verify();
    }

    @Test
    void sendsNormalizedLotNumbersAndParsesSingleBuilding() {
        server.expect(buildingRequest())
                .andRespond(withSuccess(successResponse("""
                        {
                          "mainPurpsCdNm":"공동주택",
                          "etcPurps":"아파트",
                          "bldNm":"테스트아파트",
                          "violBldgYn":"N"
                        }
                        """), MediaType.APPLICATION_JSON));

        BuildingData result = client.getBuildingData(target());

        assertThat(result).isNotNull();
        assertThat(result.getBuildingUse()).isEqualTo("공동주택, 아파트");
        assertThat(result.getBuildingType()).isEqualTo("APARTMENT");
        assertThat(result.getIsIllegalBuilding()).isFalse();
    }

    @Test
    void selectsOnlyTheRowWithTheUniqueTargetIdentity() {
        server.expect(buildingRequest())
                .andRespond(withSuccess(successResponse("""
                        [
                          {"mgmBldrgstPk":"other","mainPurpsCdNm":"업무시설","etcPurps":"사무실","bldNm":"상가"},
                          {"mgmBldrgstPk":"building","mainPurpsCdNm":"공동주택","etcPurps":"주거용 아파트","bldNm":"주택","violBldgYn":"Y"}
                        ]
                        """, 2), MediaType.APPLICATION_JSON));

        BuildingData result = client.getBuildingData(target());

        assertThat(result.getBuildingType()).isEqualTo("APARTMENT");
        assertThat(result.getIsIllegalBuilding()).isTrue();
    }

    @Test
    void rejectsAmbiguousRowsEvenWhenOneLooksMoreResidential() {
        server.expect(buildingRequest())
                .andRespond(withSuccess(successResponse("""
                        [
                          {"mgmBldrgstPk":"building","mainPurpsCdNm":"업무시설","etcPurps":"오피스텔"},
                          {"mgmBldrgstPk":"building","mainPurpsCdNm":"공동주택","etcPurps":"아파트"}
                        ]
                        """, 2), MediaType.APPLICATION_JSON));

        assertThat(client.getBuildingData(target())).isNull();
    }

    @Test
    void rejectsSingleRowWhenItsExplicitIdentityConflicts() {
        server.expect(buildingRequest())
                .andRespond(withSuccess(successResponse("""
                        {"mgmBldrgstPk":"other","mainPurpsCdNm":"공동주택","etcPurps":"아파트"}
                        """), MediaType.APPLICATION_JSON));

        assertThat(client.getBuildingData(target())).isNull();
    }

    @Test
    void matchesDongWithOrWithoutTheDongSuffix() {
        server.expect(buildingRequest())
                .andRespond(withSuccess(successResponse("""
                        [
                          {"dongNm":"102동","mainPurpsCdNm":"공동주택","etcPurps":"아파트"},
                          {"dongNm":"101","mainPurpsCdNm":"단독주택","etcPurps":"다가구주택"}
                        ]
                        """, 2), MediaType.APPLICATION_JSON));

        BuildingData result = client.getBuildingData(target(), "101동 1203호");

        assertThat(result).isNotNull();
        assertThat(result.getBuildingType()).isEqualTo("MULTI_FAMILY");
    }

    @Test
    void doesNotTreatAStandaloneSubNumberAsTargetIdentity() {
        server.expect(buildingRequest())
                .andRespond(withSuccess(successResponse("""
                        [
                          {"naSubBun":"0","mainPurpsCdNm":"공동주택","etcPurps":"아파트"},
                          {"mainPurpsCdNm":"업무시설","etcPurps":"사무실"}
                        ]
                        """, 2), MediaType.APPLICATION_JSON));

        assertThat(client.getBuildingData(target())).isNull();
    }

    @Test
    void skipsRequestWhenTargetIsMissing() {
        assertThat(client.getBuildingData(null)).isNull();
    }

    @Test
    void skipsRequestWhenApiKeyIsMissing() {
        ReflectionTestUtils.setField(client, "apiKey", "  ");

        assertThat(client.getBuildingData(target())).isNull();
    }

    @Test
    void skipsRequestWhenRequiredRegionCodeIsMissing() {
        AnalysisTargetDTO missingCode = new AnalysisTargetDTO(
                "원본", "서울 강남구 테헤란로 1", "1168010100",
                "11680", null, "737", "84", "1", "0", "building"
        );

        assertThat(client.getBuildingData(missingCode)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"response\":{\"header\":{\"resultCode\":\"99\",\"resultMsg\":\"error\"}}}",
            "{\"response\":{\"header\":{\"resultCode\":\"00\"}}}",
            "{\"response\":{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":{}}}}",
            "{\"response\":{\"header\":{\"resultCode\":\"00\"},\"body\":{\"totalCount\":-1,\"items\":{}}}}",
            "{\"response\":{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":{\"item\":{\"mainPurpsCdNm\":\"공동주택\"}}}}}"
    })
    void malformedOrUnsuccessfulResponseBecomesUnavailable(String response) {
        server.expect(buildingRequest())
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        assertThat(client.getBuildingData(target())).isNull();
    }

    @Test
    void transportFailureBecomesUnavailable() {
        server.expect(buildingRequest())
                .andRespond(withException(new IOException("network down")));

        assertThat(client.getBuildingData(target())).isNull();
    }

    @Test
    void sendsMountainCodeWhenAddressProviderConfirmedIt() {
        server.expect(buildingRequest(1, "1"))
                .andRespond(withSuccess(successResponse("""
                        {"mgmBldrgstPk":"building","mainPurpsCdNm":"단독주택"}
                        """), MediaType.APPLICATION_JSON));

        AnalysisTargetDTO mountain = new AnalysisTargetDTO(
                "원본", "서울 강남구 테헤란로 1", "1168010100",
                "11680", "10100", "737", "84", "1", "0", "building",
                null, null, "1"
        );

        assertThat(client.getBuildingData(mountain)).isNotNull();
    }

    @Test
    void followsTotalCountAcrossPagesBeforeSelectingTarget() {
        server.expect(buildingRequest(1, null))
                .andRespond(withSuccess(successResponse("""
                        {"mgmBldrgstPk":"other","mainPurpsCdNm":"업무시설"}
                        """, 2), MediaType.APPLICATION_JSON));
        server.expect(buildingRequest(2, null))
                .andRespond(withSuccess(successResponse("""
                        {"mgmBldrgstPk":"building","mainPurpsCdNm":"단독주택","etcPurps":"다가구주택","totArea":"123.45"}
                        """, 2), MediaType.APPLICATION_JSON));

        BuildingData result = client.getBuildingData(target());

        assertThat(result).isNotNull();
        assertThat(result.getBuildingType()).isEqualTo("MULTI_FAMILY");
        assertThat(result.getTransactionAreaSqm()).isEqualByComparingTo("123.45");
    }

    @Test
    void rejectsPagesThatReturnMoreRowsThanDeclared() {
        server.expect(buildingRequest(1, null))
                .andRespond(withSuccess(successResponse("""
                        [
                          {"mgmBldrgstPk":"other-1","mainPurpsCdNm":"업무시설"},
                          {"mgmBldrgstPk":"other-2","mainPurpsCdNm":"업무시설"}
                        ]
                        """, 3), MediaType.APPLICATION_JSON));
        server.expect(buildingRequest(2, null))
                .andRespond(withSuccess(successResponse("""
                        [
                          {"mgmBldrgstPk":"building","mainPurpsCdNm":"공동주택","etcPurps":"아파트"},
                          {"mgmBldrgstPk":"other-3","mainPurpsCdNm":"업무시설"}
                        ]
                        """, 3), MediaType.APPLICATION_JSON));

        assertThat(client.getBuildingData(target())).isNull();
    }

    private AnalysisTargetDTO target() {
        return new AnalysisTargetDTO(
                "원본", "서울 강남구 테헤란로 1", "1168010100",
                "11680", "10100", "737", "84", "1", "0", "building"
        );
    }

    private RequestMatcher buildingRequest() {
        return buildingRequest(1, null);
    }

    private RequestMatcher buildingRequest(int pageNo, String platGbCd) {
        return request -> {
            String expected = EXPECTED_URL.replace("pageNo=1", "pageNo=" + pageNo);
            if (platGbCd != null) {
                expected = expected.replace(
                        "&bun=0737",
                        "&platGbCd=" + platGbCd + "&bun=0737"
                );
            }
            assertThat(request.getURI()).isEqualTo(URI.create(expected));
            assertThat(request.getMethod()).isEqualTo(org.springframework.http.HttpMethod.GET);
        };
    }

    private String successResponse(String itemJson) {
        return successResponse(itemJson, 1);
    }

    private String successResponse(String itemJson, Integer totalCount) {
        String countJson = totalCount != null
                ? "\"totalCount\":" + totalCount + ","
                : "";
        return """
                {
                  "response": {
                    "header": {"resultCode":"00","resultMsg":"OK"},
                    "body": {%s"items":{"item": %s}}
                  }
                }
                """.formatted(countJson, itemJson);
    }
}
