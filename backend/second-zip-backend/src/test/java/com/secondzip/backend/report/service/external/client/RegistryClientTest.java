package com.secondzip.backend.report.service.external.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.external.RegistryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryClientTest {

    private CapturingRestTemplate restTemplate;
    private RegistryClient client;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        restTemplate = new CapturingRestTemplate(objectMapper.writeValueAsString(successBody()));
        client = new RegistryClient(
                restTemplate,
                new FixedTokenProvider(),
                objectMapper,
                new RegistryRequestFactory(),
                new RegistryDataParser()
        );

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        ReflectionTestUtils.setField(
                client,
                "publicKey",
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
        );
        ReflectionTestUtils.setField(client, "loginPhoneNo", "01000000000");
        ReflectionTestUtils.setField(client, "loginPassword", "password");
        ReflectionTestUtils.setField(client, "ePrepayNo", "prepay");
        ReflectionTestUtils.setField(client, "ePrepayPass", "prepay-password");
        ReflectionTestUtils.setField(client, "cacheTtlSeconds", 300L);
        ReflectionTestUtils.setField(client, "registryEnabled", true);
        ReflectionTestUtils.setField(
                client,
                "registryBaseUrl",
                "https://development.codef.io"
        );
    }

    @Test
    void sendsRoadBuildingNumberAndRequiredRegistryOptions() {
        AnalysisTarget target = target();

        RegistryData result = client.getRegistryData(target, "101동 1203호");

        assertNotNull(result);
        assertEquals(100_000_000L, result.getMortgageAmount());
        assertTrue(result.getHasSeizure());
        assertFalse(result.getHasTrustRegistration());
        assertEquals("홍길동", result.getOwnerName());

        Map<String, Object> request = restTemplate.requestBody;
        assertEquals("152-1", request.get("addr_buildingNumber"));
        assertEquals("1", request.get("applicationType"));
        assertEquals("1", request.get("jointMortgageJeonseYN"));
        assertEquals("101", request.get("dong"));
        assertEquals("1203", request.get("ho"));
    }

    @Test
    void cachesSuccessfulPaidLookup() {
        AnalysisTarget target = target();

        client.getRegistryData(target, "101동 1203호");
        client.getRegistryData(target, "101동 1203호");

        assertEquals(1, restTemplate.callCount);
    }

    @Test
    void refreshesTokenAndRetriesOnlyOnceOnUnauthorized() {
        restTemplate.unauthorizedFirst = true;

        RegistryData result = client.getRegistryData(target(), "101동 1203호");

        assertNotNull(result);
        assertEquals(2, restTemplate.callCount);
    }

    @Test
    void generalHousingQueriesBuildingAndLandAndMergesOwner() {
        RegistryData result = client.getRegistryDataForAnalysis(
                targetWithLotAddress(),
                null,
                "SINGLE_FAMILY"
        );

        assertNotNull(result);
        assertEquals("홍길동", result.getOwnerName());
        assertEquals("홍길동", result.getLandOwnerName());
        assertEquals(2, restTemplate.requestBodies.size());
        assertEquals("3", restTemplate.requestBodies.get(0).get("realtyType"));
        assertEquals("2", restTemplate.requestBodies.get(1).get("realtyType"));
        assertEquals("대치동", restTemplate.requestBodies.get(1).get("addr_dong"));
        assertEquals("737", restTemplate.requestBodies.get(1).get("addr_lotNumber"));
    }

    private AnalysisTarget target() {
        return new AnalysisTarget(
                "서울 강남구 테헤란로 152-1",
                "서울 강남구 테헤란로 152-1",
                "1168010100",
                "11680",
                "10100",
                "737",
                "",
                "152",
                "1",
                ""
        );
    }

    private AnalysisTarget targetWithLotAddress() {
        AnalysisTarget target = target();
        return new AnalysisTarget(
                target.originalAddress(),
                target.roadAddress(),
                target.legalDongCode(),
                target.sigunguCode(),
                target.bjdongCode(),
                target.mainNo(),
                target.subNo(),
                target.roadBuildingMainNo(),
                target.roadBuildingSubNo(),
                target.buildingManagementNo(),
                "대치동",
                "서울 강남구 대치동 737"
        );
    }

    private Map<String, Object> successBody() {
        Map<String, Object> detail = Map.of(
                "resContents",
                "소유자 홍길동 채권최고액 금 100,000,000원 가압류"
        );
        Map<String, Object> content = Map.of("resDetailList", List.of(detail));
        Map<String, Object> summary = Map.of("resContentsList", List.of(content));
        Map<String, Object> registerEntry =
                Map.of("resRegistrationSumList", List.of(summary));
        return Map.of(
                "result", Map.of("code", "CF-00000"),
                "data", Map.of("resRegisterEntriesList", List.of(registerEntry))
        );
    }

    private static class FixedTokenProvider extends CodefTokenProvider {
        FixedTokenProvider() {
            super(new RestTemplate());
        }

        @Override
        public synchronized String getToken() {
            return "token";
        }
    }

    private static class CapturingRestTemplate extends RestTemplate {
        private final String responseBody;
        private Map<String, Object> requestBody;
        private final List<Map<String, Object>> requestBodies =
                new ArrayList<>();
        private int callCount;
        private boolean unauthorizedFirst;

        private CapturingRestTemplate(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<T> postForEntity(
                String url,
                Object request,
                Class<T> responseType,
                Object... uriVariables
        ) {
            callCount++;
            if (unauthorizedFirst && callCount == 1) {
                throw HttpClientErrorException.create(
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                );
            }
            requestBody = (Map<String, Object>) ((HttpEntity<?>) request).getBody();
            requestBodies.add(requestBody);
            return ResponseEntity.ok((T) responseBody);
        }
    }
}
