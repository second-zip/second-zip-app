package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AddressCandidateDTO;
import com.secondzip.backend.report.dto.AnalysisTargetDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressClientTest {

    private AddressClient addressClient;

    @BeforeEach
    void setUp() {
        // Spring Context를 띄우지 않고 객체를 직접 생성하여 빠르게 테스트합니다.
        RestTemplate restTemplate = new RestTemplate();
        addressClient = new AddressClient(restTemplate);

        String myKakaoApiKey = System.getenv("KAKAO_REST_API_KEY");

        // @Value 로 주입되는 private 필드에 리플렉션으로 값을 강제 주입합니다.
        ReflectionTestUtils.setField(addressClient, "kakaoApiKey", myKakaoApiKey);
    }

    @Test
    @DisplayName("카카오 주소 검색 API 실제 호출 및 파싱 테스트")
    @EnabledIfEnvironmentVariable(named = "KAKAO_REST_API_KEY", matches = ".+")
    void testStandardizeWithRealApi() {
        // 테스트할 주소 (본번/부번이 명확한 주소)
        String inputAddress = "서울특별시 강남구 테헤란로 152";

        // API 호출 및 데이터 파싱
        AnalysisTargetDTO target = addressClient.search(inputAddress).stream()
                .findFirst()
                .map(AddressCandidateDTO::target)
                .orElse(null);

        // 결과 출력 및 검증
        assertNotNull(target, "주소 검색 결과가 없습니다. API 키와 테스트 주소를 확인하세요.");
        System.out.println("====== [카카오 API 파싱 결과] ======");
        System.out.println("원본 주소: " + target.originalAddress());
        System.out.println("도로명 주소: " + target.roadAddress());
        System.out.println("법정동코드(전체): " + target.legalDongCode());
        System.out.println(" ├─ 시군구코드: " + target.sigunguCode());
        System.out.println(" └─ 읍면동코드: " + target.bjdongCode());
        System.out.println("본번(mainNo): " + target.mainNo());
        System.out.println("부번(subNo): " + target.subNo());
        System.out.println("==================================");

        // 검증 - API가 정상 호출 -> 법정동코드가 null이 아니어야 함.
        assertNotNull(target.legalDongCode(), "법정동 코드가 파싱되지 않았습니다! API 키나 응답을 확인하세요.");

        // 강남구의 시군구코드는 '11680'으로 시작. (테헤란로 기준)
        assertTrue(target.sigunguCode().startsWith("11680"), "시군구 코드가 일치하지 않습니다.");
    }
}
