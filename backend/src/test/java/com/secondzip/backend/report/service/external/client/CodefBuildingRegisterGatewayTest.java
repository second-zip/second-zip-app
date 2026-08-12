package com.secondzip.backend.report.service.external.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.AnalysisWorkflowState;
import com.secondzip.backend.report.dto.CodefTwoWayState;
import com.secondzip.backend.report.dto.request.ContinueAnalysisAuthRequest;
import com.secondzip.backend.report.dto.request.StartAnalysisAuthRequest;
import com.secondzip.backend.report.enums.AnalysisNextAction;
import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import com.secondzip.backend.report.enums.SimpleAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CodefBuildingRegisterGatewayTest {

    private CapturingRestTemplate restTemplate;
    private CodefBuildingRegisterGateway gateway;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String response = objectMapper.writeValueAsString(Map.of(
                "result", Map.of("code", "CF-03002"),
                "data", Map.of(
                        "continue2Way", true,
                        "jobIndex", 1,
                        "threadIndex", 2,
                        "jti", "test-jti",
                        "twoWayTimestamp", "123456789",
                        "extraInfo", Map.of(
                                "reqAddrList", List.of(),
                                "reqDongNumList", List.of(),
                                "reqHoNumList", List.of(),
                                "commSimpleAuth", ""
                        )
                )
        ));
        restTemplate = new CapturingRestTemplate(response);
        gateway = new CodefBuildingRegisterGateway(
                restTemplate,
                new FixedTokenProvider(),
                objectMapper
        );
        ReflectionTestUtils.setField(gateway, "enabled", true);
        ReflectionTestUtils.setField(
                gateway,
                "baseUrl",
                "https://development.codef.io"
        );
    }

    @Test
    void startsExclusiveRegisterWithBirthDateOnly() {
        BuildingRegisterGatewayResult result = gateway.start(
                workflowState(),
                BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE,
                authRequest()
        );

        assertFalse(result.isCompleted());
        assertEquals(AnalysisNextAction.SIMPLE_AUTH, result.getNextAction());
        assertEquals(
                "/v1/kr/public/mw/multiowned-buildings/possession-ledger",
                restTemplate.requestPath
        );
        assertEquals("19900101", restTemplate.requestBody.get("identity"));
        assertEquals("테헤란로 152", restTemplate.requestBody.get("address"));
        assertEquals("101", restTemplate.requestBody.get("dong"));
        assertEquals("1203", restTemplate.requestBody.get("ho"));
        assertFalse(restTemplate.requestBody.containsKey("certPassword"));
        assertFalse(restTemplate.requestBody.containsKey("userPassword"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void continuesWithExactTwoWayFields() {
        AnalysisWorkflowState state = workflowState();
        state.setPendingDocument(
                BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE
        );
        state.setNextAction(AnalysisNextAction.SIMPLE_AUTH);
        state.setTwoWayState(
                new CodefTwoWayState(1, 2, "test-jti", "123456789")
        );
        ContinueAnalysisAuthRequest request =
                new ContinueAnalysisAuthRequest();
        request.setAuthentication(authRequest());

        gateway.continueRequest(state, request);

        assertEquals(true, restTemplate.requestBody.get("is2Way"));
        assertEquals("1", restTemplate.requestBody.get("simpleAuth"));
        Map<String, Object> twoWay =
                (Map<String, Object>) restTemplate.requestBody.get(
                        "twoWayInfo"
                );
        assertEquals(1, twoWay.get("jobIndex"));
        assertEquals(2, twoWay.get("threadIndex"));
        assertEquals("test-jti", twoWay.get("jti"));
        assertEquals("123456789", twoWay.get("twoWayTimestamp"));
    }

    @Test
    void exposesCaptchaImageFromTwoWayResponse() throws Exception {
        String captchaImage = "iVBORw0KGgoAAAANSUhEUg==";
        ObjectMapper objectMapper = new ObjectMapper();
        String response = objectMapper.writeValueAsString(Map.of(
                "result", Map.of("code", "CF-03002"),
                "data", Map.of(
                        "jobIndex", 1,
                        "threadIndex", 2,
                        "jti", "test-jti",
                        "twoWayTimestamp", "123456789",
                        "extraInfo", Map.of("reqSecureNo", captchaImage)
                )
        ));
        restTemplate = new CapturingRestTemplate(response);
        gateway = new CodefBuildingRegisterGateway(
                restTemplate,
                new FixedTokenProvider(),
                objectMapper
        );
        ReflectionTestUtils.setField(gateway, "enabled", true);
        ReflectionTestUtils.setField(
                gateway,
                "baseUrl",
                "https://development.codef.io"
        );

        BuildingRegisterGatewayResult result = gateway.start(
                workflowState(),
                BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE,
                authRequest()
        );

        assertEquals(AnalysisNextAction.CAPTCHA, result.getNextAction());
        assertEquals(captchaImage, result.getCaptchaImage());
    }

    private AnalysisWorkflowState workflowState() {
        AnalysisTarget target = new AnalysisTarget(
                "서울 강남구 테헤란로 152",
                "서울 강남구 테헤란로 152",
                "1168010100",
                "11680",
                "10100",
                "737",
                "",
                "152",
                "",
                ""
        );
        return new AnalysisWorkflowState(
                "request-id",
                1L,
                target.roadAddress(),
                "101동 1203호",
                100_000_000L,
                target,
                "APARTMENT",
                List.of(
                        BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                        BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE
                ),
                AnalysisRequestStatus.AUTH_REQUIRED,
                null,
                null,
                null,
                List.of(),
                new java.util.ArrayList<>(),
                new java.util.LinkedHashMap<>(),
                1L,
                Long.MAX_VALUE,
                null,
                null,
                null
        );
    }

    private StartAnalysisAuthRequest authRequest() {
        StartAnalysisAuthRequest request = new StartAnalysisAuthRequest();
        request.setUserName("홍길동");
        request.setBirthDate("19900101");
        request.setPhoneNo("01012341234");
        request.setProvider(SimpleAuthProvider.KAKAO);
        request.setConsent(true);
        return request;
    }

    private static class FixedTokenProvider extends CodefTokenProvider {
        private FixedTokenProvider() {
            super(new RestTemplate());
        }

        @Override
        public synchronized String getToken() {
            return "token";
        }
    }

    private static class CapturingRestTemplate extends RestTemplate {
        private final String response;
        private String requestPath;
        private Map<String, Object> requestBody;

        private CapturingRestTemplate(String response) {
            this.response = response;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<T> postForEntity(
                String url,
                Object request,
                Class<T> responseType,
                Object... uriVariables
        ) {
            requestPath = java.net.URI.create(url).getPath();
            requestBody = (Map<String, Object>) ((HttpEntity<?>) request).getBody();
            return ResponseEntity.ok((T) response);
        }
    }
}
