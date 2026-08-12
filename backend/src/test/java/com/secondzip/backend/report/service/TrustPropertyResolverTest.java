package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.external.RegistryData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrustPropertyResolverTest {

    private final TrustPropertyResolver resolver = new TrustPropertyResolver();

    @Test
    @DisplayName("등기에 신탁등기가 있으면 신탁주택으로 판정한다")
    void trustRegistrationMeansTrustProperty() {
        RegistryData registry = new RegistryData();
        registry.setHasTrustRegistration(true);

        assertTrue(resolver.resolve(registry));
    }

    @Test
    @DisplayName("신탁등기 표시가 없어도 소유자가 신탁회사면 신탁주택으로 판정한다")
    void trustCompanyOwnerMeansTrustProperty() {
        RegistryData registry = new RegistryData();
        registry.setHasTrustRegistration(false);
        registry.setOwnerType("TRUST_COMPANY");

        assertTrue(resolver.resolve(registry));
    }

    @Test
    @DisplayName("신탁등기도 없고 개인 소유면 신탁주택이 아니다")
    void individualOwnerWithoutTrustIsNotTrustProperty() {
        RegistryData registry = new RegistryData();
        registry.setHasTrustRegistration(false);
        registry.setOwnerType("INDIVIDUAL");

        assertFalse(resolver.resolve(registry));
    }

    @Test
    @DisplayName("판정 근거가 없으면 신탁주택이 아니다 - 체크리스트에 TRUST_PROPERTY를 붙이지 않는다")
    void missingDataIsNotTrustProperty() {
        assertFalse(resolver.resolve(null));
        assertFalse(resolver.resolve(new RegistryData()));
    }
}
