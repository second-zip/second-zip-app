package com.secondzip.backend.report.service.external.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.report.dto.AnalysisTargetDTO;
import com.secondzip.backend.report.dto.external.RegistryData;
import com.secondzip.backend.report.enums.RegistryDocumentType;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryClientTest {

    private CapturingRestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private String encodedPublicKey;
    private RegistryClient client;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        restTemplate = new CapturingRestTemplate(objectMapper.writeValueAsString(successBody()));

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        encodedPublicKey =
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        client = clientWith(new InMemoryRegistryDataCache());
    }

    /** 캐시 구현만 바꿔가며 클라이언트를 만든다. 나머지 설정은 동일하다. */
    private RegistryClient clientWith(RegistryDataCache cache) {
        RegistryClient created = new RegistryClient(
                restTemplate,
                new FixedTokenProvider(),
                objectMapper,
                new RegistryRequestFactory(),
                new RegistryDataParser(),
                cache
        );
        ReflectionTestUtils.setField(created, "publicKey", encodedPublicKey);
        ReflectionTestUtils.setField(created, "loginPhoneNo", "01000000000");
        ReflectionTestUtils.setField(created, "loginPassword", "password");
        ReflectionTestUtils.setField(created, "ePrepayNo", "prepay");
        ReflectionTestUtils.setField(created, "ePrepayPass", "prepay-password");
        ReflectionTestUtils.setField(created, "registryEnabled", true);
        ReflectionTestUtils.setField(
                created,
                "registryBaseUrl",
                "https://development.codef.io"
        );
        return created;
    }

    /** Redis 대역. 실제 캐시 동작(적중/미적중)만 흉내 낸다. */
    private static class InMemoryRegistryDataCache implements RegistryDataCache {
        private final Map<String, RegistryData> store = new HashMap<>();

        @Override
        public RegistryData find(String cacheKey) {
            return store.get(cacheKey);
        }

        @Override
        public void put(String cacheKey, RegistryData data) {
            store.put(cacheKey, data);
        }
    }

    /** Redis가 죽은 상황. 캐시 실패가 분석을 막지 않는지 확인하는 용도. */
    private static class FailingRegistryDataCache implements RegistryDataCache {
        @Override
        public RegistryData find(String cacheKey) {
            throw new IllegalStateException("redis down");
        }

        @Override
        public void put(String cacheKey, RegistryData data) {
            throw new IllegalStateException("redis down");
        }
    }

    @Test
    void sendsRoadBuildingNumberAndRequiredRegistryOptions() {
        AnalysisTargetDTO target = target();

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
        AnalysisTargetDTO target = target();

        client.getRegistryData(target, "101동 1203호");
        client.getRegistryData(target, "101동 1203호");

        assertEquals(1, restTemplate.callCount);
    }

    @Test
    void continuesWhenCacheIsUnavailable() {
        RegistryClient clientWithoutCache = clientWith(new FailingRegistryDataCache());

        RegistryData result =
                clientWithoutCache.getRegistryData(target(), "101동 1203호");

        assertNotNull(result, "캐시가 죽어도 등기 조회 자체는 성공해야 한다");
        assertEquals(100_000_000L, result.getMortgageAmount());
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

    @Test
    void generalHousingMergesLandMortgageAndInfringement() throws Exception {
        restTemplate.respondWith(
                objectMapper.writeValueAsString(successBody(
                        "소유자 홍길동"
                )),
                objectMapper.writeValueAsString(successBody(
                        "소유자 홍길동\n채권최고액 금 300,000,000원\n처분금지가처분"
                ))
        );

        RegistryData result = client.getRegistryDataForAnalysis(
                targetWithLotAddress(), null, "MULTI_FAMILY"
        );

        assertNotNull(result);
        assertEquals(300_000_000L, result.getMortgageAmount());
        assertTrue(result.getHasSeizure());
        assertEquals("홍길동", result.getLandOwnerName());
    }

    @Test
    void generalHousingKeepsBothPositiveDocumentMortgagesUnknown() throws Exception {
        restTemplate.respondWith(
                objectMapper.writeValueAsString(successBody(
                        "소유자 홍길동\n채권최고액 금 100,000,000원"
                )),
                objectMapper.writeValueAsString(successBody(
                        "소유자 홍길동\n채권최고액 금 300,000,000원"
                ))
        );

        RegistryData result = client.getRegistryDataForAnalysis(
                targetWithLotAddress(), null, "SINGLE_FAMILY"
        );

        assertNotNull(result);
        assertNull(
                result.getMortgageAmount(),
                "건물·토지의 서로 다른 금액을 합치면 중복 계상할 수 있다"
        );
    }

    @Test
    void generalHousingTreatsIdenticalBuildingAndLandAmountAsJointCollateral() throws Exception {
        restTemplate.respondWith(
                objectMapper.writeValueAsString(successBody(
                        "소유자 홍길동\n채권최고액 금 300,000,000원"
                )),
                objectMapper.writeValueAsString(successBody(
                        "소유자 홍길동\n채권최고액 금 300,000,000원"
                ))
        );

        RegistryData result = client.getRegistryDataForAnalysis(
                targetWithLotAddress(), null, "SINGLE_FAMILY"
        );

        assertNotNull(result);
        assertEquals(
                300_000_000L,
                result.getMortgageAmount(),
                "단독·다가구의 근저당은 대부분 건물·토지 공동담보라 양쪽에 같은 금액이 "
                        + "적힌다. 이것까지 미확인으로 보내면 근저당 금액이 거의 항상 사라진다"
        );
    }

    @Test
    void generalHousingKeepsPartialMortgageAsUnknown() throws Exception {
        restTemplate.respondWith(
                objectMapper.writeValueAsString(successBody(
                        "소유자 홍길동\n채권최고액 금 (판독불가) 원"
                )),
                objectMapper.writeValueAsString(successBody(
                        "소유자 홍길동\n채권최고액 금 300,000,000원"
                ))
        );

        RegistryData result = client.getRegistryDataForAnalysis(
                targetWithLotAddress(), null, "SINGLE_FAMILY"
        );

        assertNotNull(result);
        assertNull(result.getMortgageAmount());
    }

    @Test
    void generalHousingRejectsOneSidedOrHeaderOnlyResponse() throws Exception {
        restTemplate.respondWith(
                objectMapper.writeValueAsString(successBody(
                        "소유자 홍길동\n채권최고액 금 100,000,000원"
                )),
                objectMapper.writeValueAsString(successBody(
                        "등기사항전부증명서(열람용)"
                ))
        );

        RegistryData result = client.getRegistryDataForAnalysis(
                targetWithLotAddress(), null, "SINGLE_FAMILY"
        );

        assertNull(result);
    }

    @Test
    void buildingFailureShortCircuitsPaidLandLookup() throws Exception {
        restTemplate.respondWith(objectMapper.writeValueAsString(successBody(
                "등기사항전부증명서(열람용)"
        )));

        RegistryData result = client.getRegistryDataForAnalysis(
                targetWithLotAddress(), null, "SINGLE_FAMILY"
        );

        assertNull(result);
        assertEquals(1, restTemplate.callCount);
        assertEquals(1, restTemplate.requestBodies.size());
        assertEquals("3", restTemplate.requestBodies.get(0).get("realtyType"));
    }

    @Test
    void landCacheKeySeparatesOrdinaryAndMountainParcels() {
        client.getRegistryData(
                targetWithLotAddress("0"),
                null,
                RegistryDocumentType.LAND
        );
        client.getRegistryData(
                targetWithLotAddress("1"),
                null,
                RegistryDocumentType.LAND
        );

        assertEquals(2, restTemplate.callCount);
        assertEquals("737", restTemplate.requestBodies.get(0).get("addr_lotNumber"));
        assertEquals("산737", restTemplate.requestBodies.get(1).get("addr_lotNumber"));
    }

    @Test
    void headerOnlySuccessfulResponseIsRejected() throws Exception {
        restTemplate.respondWith(objectMapper.writeValueAsString(successBody(
                "등기사항전부증명서(열람용)"
        )));

        assertNull(client.getRegistryData(target(), "101동 1203호"));
    }

    private AnalysisTargetDTO target() {
        return new AnalysisTargetDTO(
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

    private AnalysisTargetDTO targetWithLotAddress() {
        return targetWithLotAddress("0");
    }

    private AnalysisTargetDTO targetWithLotAddress(String platGbCd) {
        AnalysisTargetDTO target = target();
        return new AnalysisTargetDTO(
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
                "서울 강남구 대치동 737",
                platGbCd
        );
    }

    private Map<String, Object> successBody() {
        return successBody(
                "소유자 홍길동 채권최고액 금 100,000,000원 가압류"
        );
    }

    private Map<String, Object> successBody(String registryText) {
        Map<String, Object> detail = Map.of(
                "resContents",
                registryText
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
        private List<String> responseBodies;
        private Map<String, Object> requestBody;
        private final List<Map<String, Object>> requestBodies =
                new ArrayList<>();
        private int callCount;
        private int successfulCallCount;
        private boolean unauthorizedFirst;

        private CapturingRestTemplate(String responseBody) {
            this.responseBodies = List.of(responseBody);
        }

        private void respondWith(String... bodies) {
            this.responseBodies = List.of(bodies);
            this.successfulCallCount = 0;
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
            String responseBody = responseBodies.get(Math.min(
                    successfulCallCount++,
                    responseBodies.size() - 1
            ));
            return ResponseEntity.ok((T) responseBody);
        }
    }
}
