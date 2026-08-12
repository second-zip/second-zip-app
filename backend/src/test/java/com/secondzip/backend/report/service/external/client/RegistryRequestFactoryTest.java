package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.enums.RegistryDocumentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RegistryRequestFactoryTest {

    private final RegistryRequestFactory factory = new RegistryRequestFactory();

    @Test
    void collectiveUsesRoadAddressAndDongHo() {
        Map<String, Object> body =
                factory.create(
                        target(),
                        RegistryDocumentType.COLLECTIVE,
                        "101동 1203호",
                        "01000000000",
                        "encrypted",
                        "prepay",
                        "prepay-pass"
                );

        assertEquals("3", body.get("inquiryType"));
        assertEquals("1", body.get("realtyType"));
        assertEquals("152-1", body.get("addr_buildingNumber"));
        assertEquals("101", body.get("dong"));
        assertEquals("1203", body.get("ho"));
    }

    @Test
    void generalBuildingUsesRoadAddressWithoutDongHo() {
        Map<String, Object> body =
                factory.create(
                        target(),
                        RegistryDocumentType.BUILDING,
                        null,
                        "01000000000",
                        "encrypted",
                        "prepay",
                        "prepay-pass"
                );

        assertEquals("3", body.get("inquiryType"));
        assertEquals("3", body.get("realtyType"));
        assertFalse(body.containsKey("dong"));
        assertFalse(body.containsKey("ho"));
    }

    @Test
    void landUsesLotAddressFields() {
        Map<String, Object> body =
                factory.create(
                        target(),
                        RegistryDocumentType.LAND,
                        null,
                        "01000000000",
                        "encrypted",
                        "prepay",
                        "prepay-pass"
                );

        assertEquals("2", body.get("inquiryType"));
        assertEquals("2", body.get("realtyType"));
        assertEquals("대치동", body.get("addr_dong"));
        assertEquals("737-1", body.get("addr_lotNumber"));
        assertFalse(body.containsKey("addr_roadName"));
    }

    private AnalysisTarget target() {
        return new AnalysisTarget(
                "서울 강남구 테헤란로 152-1",
                "서울 강남구 테헤란로 152-1",
                "1168010100",
                "11680",
                "10100",
                "737",
                "1",
                "152",
                "1",
                "",
                "대치동",
                "서울 강남구 대치동 737-1"
        );
    }
}
