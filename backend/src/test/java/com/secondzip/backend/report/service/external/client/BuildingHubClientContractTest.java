package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AnalysisTarget;
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
        assertThat(result.getBuildingUse()).isEqualTo("공동주택");
        assertThat(result.getBuildingType()).isEqualTo("APARTMENT");
        assertThat(result.getIsIllegalBuilding()).isFalse();
    }

    @Test
    void selectsMostResidentialItemFromMultipleResults() {
        server.expect(buildingRequest())
                .andRespond(withSuccess(successResponse("""
                        [
                          {"mainPurpsCdNm":"업무시설","etcPurps":"사무실","bldNm":"상가"},
                          {"mainPurpsCdNm":"공동주택","etcPurps":"주거용 아파트","bldNm":"주택","mainAtchGbCdNm":"주건축물","violBldgYn":"Y"}
                        ]
                        """), MediaType.APPLICATION_JSON));

        BuildingData result = client.getBuildingData(target());

        assertThat(result.getBuildingType()).isEqualTo("APARTMENT");
        assertThat(result.getIsIllegalBuilding()).isTrue();
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
        AnalysisTarget missingCode = new AnalysisTarget(
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
            "{\"response\":{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":{}}}}"
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

    private AnalysisTarget target() {
        return new AnalysisTarget(
                "원본", "서울 강남구 테헤란로 1", "1168010100",
                "11680", "10100", "737", "84", "1", "0", "building"
        );
    }

    private RequestMatcher buildingRequest() {
        return request -> {
            assertThat(request.getURI()).isEqualTo(URI.create(EXPECTED_URL));
            assertThat(request.getMethod()).isEqualTo(org.springframework.http.HttpMethod.GET);
        };
    }

    private String successResponse(String itemJson) {
        return """
                {
                  "response": {
                    "header": {"resultCode":"00","resultMsg":"OK"},
                    "body": {"items":{"item": %s}}
                  }
                }
                """.formatted(itemJson);
    }
}
