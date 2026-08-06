package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.report.dto.response.ExternalApiReadinessResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalApiReadinessServiceTest {

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