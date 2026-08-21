package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AnalysisTargetDTO;
import com.secondzip.backend.report.enums.RegistryDocumentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void collectivePreservesNonNumericDongAndHoTokens() {
        Map<String, Object> alphanumeric = factory.create(
                target(),
                RegistryDocumentType.COLLECTIVE,
                "가동 B101호",
                "01000000000",
                "encrypted",
                "prepay",
                "prepay-pass"
        );
        Map<String, Object> hyphenated = factory.create(
                target(),
                RegistryDocumentType.COLLECTIVE,
                "101-1호",
                "01000000000",
                "encrypted",
                "prepay",
                "prepay-pass"
        );
        Map<String, Object> addressPrefixed = factory.create(
                target(),
                RegistryDocumentType.COLLECTIVE,
                "역삼동 101동 B101호",
                "01000000000",
                "encrypted",
                "prepay",
                "prepay-pass"
        );

        assertEquals("가", alphanumeric.get("dong"));
        assertEquals("B101", alphanumeric.get("ho"));
        assertEquals("101-1", hyphenated.get("ho"));
        assertEquals("101", addressPrefixed.get("dong"));
        assertEquals("B101", addressPrefixed.get("ho"));
    }

    @Test
    void collectiveRejectsDongWithoutUnitNumber() {
        assertThrows(
                IllegalArgumentException.class,
                () -> factory.create(
                        target(),
                        RegistryDocumentType.COLLECTIVE,
                        "101동",
                        "01000000000",
                        "encrypted",
                        "prepay",
                        "prepay-pass"
                )
        );
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

    @Test
    void mountainLandPrefixesLotNumberWithSan() {
        Map<String, Object> body = factory.create(
                targetWithPlatGbCd("1"),
                RegistryDocumentType.LAND,
                null,
                "01000000000",
                "encrypted",
                "prepay",
                "prepay-pass"
        );

        assertEquals("산737-1", body.get("addr_lotNumber"));
    }

    @Test
    void landRejectsUnknownPlatGbCd() {
        assertThrows(
                IllegalArgumentException.class,
                () -> factory.create(
                        targetWithPlatGbCd("9"),
                        RegistryDocumentType.LAND,
                        null,
                        "01000000000",
                        "encrypted",
                        "prepay",
                        "prepay-pass"
                )
        );
    }

    @Test
    void sejongRoadAddressAllowsEmptySigungu() {
        AnalysisTargetDTO sejong = new AnalysisTargetDTO(
                "세종특별자치시 한누리대로 2130",
                "세종특별자치시 한누리대로 2130",
                "3611010100",
                "36110",
                "10100",
                "1",
                "0",
                "2130",
                "0",
                ""
        );

        Map<String, Object> body = factory.create(
                sejong,
                RegistryDocumentType.BUILDING,
                null,
                "01000000000",
                "encrypted",
                "prepay",
                "prepay-pass"
        );

        assertEquals("세종특별자치시", body.get("addr_sido"));
        assertEquals("", body.get("addr_sigungu"));
        assertEquals("한누리대로", body.get("addr_roadName"));
        assertEquals("2130", body.get("addr_buildingNumber"));
    }

    @Test
    void nonSejongRoadAddressStillRequiresSigungu() {
        AnalysisTargetDTO missingSigungu = new AnalysisTargetDTO(
                "서울특별시 테헤란로 152",
                "서울특별시 테헤란로 152",
                "1168010100",
                "11680",
                "10100",
                "737",
                "0",
                "152",
                "0",
                ""
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> factory.create(
                        missingSigungu,
                        RegistryDocumentType.BUILDING,
                        null,
                        "01000000000",
                        "encrypted",
                        "prepay",
                        "prepay-pass"
                )
        );
    }

    private AnalysisTargetDTO target() {
        return new AnalysisTargetDTO(
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

    private AnalysisTargetDTO targetWithPlatGbCd(String platGbCd) {
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
                target.legalDongName(),
                target.lotAddress(),
                platGbCd
        );
    }
}
