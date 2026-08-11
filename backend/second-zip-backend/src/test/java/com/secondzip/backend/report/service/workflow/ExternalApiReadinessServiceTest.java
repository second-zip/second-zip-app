package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.report.dto.response.ExternalApiReadinessResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalApiReadinessServiceTest {

    private static final String MODE_KEY = "EXTERNAL_API_MODE";

    private String originalMode;

    /**
     * {@code ExternalApiModeResolver}는 시스템 프로퍼티 → 환경변수 →
     * {@code src/main/resources/.env} 순으로 모드를 읽는다.
     *
     * <p>테스트에서 모드를 지정하지 않으면 실행 환경의 {@code .env}를 그대로 읽어버려,
     * 로컬에 {@code EXTERNAL_API_MODE=real}이 있으면 테스트가 깨진다.
     * 시스템 프로퍼티가 가장 우선이므로 여기서 명시적으로 고정한다.
     */
    @BeforeEach
    void fixExternalApiMode() {
        originalMode = System.getProperty(MODE_KEY);
        System.setProperty(MODE_KEY, "mock");
    }

    @AfterEach
    void restoreExternalApiMode() {
        if (originalMode == null) {
            System.clearProperty(MODE_KEY);
        } else {
            System.setProperty(MODE_KEY, originalMode);
        }
    }

    @Test
    @DisplayName("설정이 모두 갖춰져도 EXTERNAL_API_MODE가 real이 아니면 준비 완료가 아니다")
    void notReadyWhenExternalApiModeIsNotReal() {
        ExternalApiReadinessService service = fullyConfiguredService();

        ExternalApiReadinessResponse result = service.check();

        assertEquals("DEMO", result.getCodefEnvironment());
        assertTrue(result.getMissingConfigurations().isEmpty());
        assertEquals(1, result.getWarnings().size());
        assertFalse(result.isReady());
    }

    @Test
    @DisplayName("설정이 모두 갖춰지고 EXTERNAL_API_MODE가 real이면 준비 완료다")
    void readyWhenEverythingIsConfiguredAndModeIsReal() {
        System.setProperty(MODE_KEY, "real");
        ExternalApiReadinessService service = fullyConfiguredService();

        ExternalApiReadinessResponse result = service.check();

        assertTrue(result.getMissingConfigurations().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
        assertTrue(result.isReady());
    }

    @Test
    @DisplayName("설정 키가 하나라도 비면 누락 목록에 포함된다")
    void reportsMissingConfiguration() {
        ExternalApiReadinessService service = fullyConfiguredService();
        ReflectionTestUtils.setField(service, "kakaoKey", "");

        ExternalApiReadinessResponse result = service.check();

        assertEquals(1, result.getMissingConfigurations().size());
        assertEquals(
                "KAKAO_REST_API_KEY",
                result.getMissingConfigurations().get(0)
        );
        assertFalse(result.isReady());
    }

    private ExternalApiReadinessService fullyConfiguredService() {
        ExternalApiReadinessService service =
                new ExternalApiReadinessService();
        for (String field : new String[]{
                "kakaoKey",
                "buildingHubKey",
                "realtyPriceKey",
                "codefClientId",
                "codefClientSecret",
                "codefPublicKey",
                "codefLoginPhoneNo",
                "codefLoginPassword",
                "codefPrepayNo",
                "codefPrepayPass"
        }) {
            ReflectionTestUtils.setField(service, field, "configured");
        }
        ReflectionTestUtils.setField(service, "registryEnabled", true);
        ReflectionTestUtils.setField(
                service,
                "buildingRegisterEnabled",
                true
        );
        ReflectionTestUtils.setField(
                service,
                "registryBaseUrl",
                "https://development.codef.io"
        );
        ReflectionTestUtils.setField(
                service,
                "buildingRegisterBaseUrl",
                "https://development.codef.io"
        );
        return service;
    }
}