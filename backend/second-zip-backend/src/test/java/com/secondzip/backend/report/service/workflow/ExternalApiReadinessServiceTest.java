package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.report.dto.response.ExternalApiReadinessResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalApiReadinessServiceTest {

    @Test
    void readyOnlyWhenAllKeysArePresentAndBothUrlsAreDemo() {
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

        ExternalApiReadinessResponse result = service.check();

        assertTrue(result.isReady());
        assertEquals("DEMO", result.getCodefEnvironment());
        assertTrue(result.getMissingConfigurations().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
    }
}
