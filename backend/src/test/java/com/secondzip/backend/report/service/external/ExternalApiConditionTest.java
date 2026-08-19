package com.secondzip.backend.report.service.external;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalApiConditionTest {

    private final String originalMode =
            System.getProperty("EXTERNAL_API_MODE");

    @AfterEach
    void restoreMode() {
        if (originalMode == null) {
            System.clearProperty("EXTERNAL_API_MODE");
        } else {
            System.setProperty("EXTERNAL_API_MODE", originalMode);
        }
    }

    @Test
    @DisplayName("real 모드에서는 실제 어댑터만 활성화한다")
    void activatesRealAdaptersOnlyInRealMode() {
        System.setProperty("EXTERNAL_API_MODE", "real");

        assertTrue(new RealApiCondition().matches(null, null));
        assertFalse(new MockApiCondition().matches(null, null));
    }

    @Test
    @DisplayName("mock 모드에서는 Mock 어댑터만 활성화한다")
    void activatesMockAdaptersOutsideRealMode() {
        System.setProperty("EXTERNAL_API_MODE", "mock");

        assertFalse(new RealApiCondition().matches(null, null));
        assertTrue(new MockApiCondition().matches(null, null));
    }
}
