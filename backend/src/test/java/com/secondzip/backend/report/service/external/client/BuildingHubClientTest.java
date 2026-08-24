package com.secondzip.backend.report.service.external.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingHubClientTest {

    @Test
    @DisplayName("건축물대장 본번과 부번을 4자리로 정규화한다")
    void normalizeLotNumbers() {
        assertEquals("0737", BuildingHubClient.normalizeLotNumber("737"));
        assertEquals("0084", BuildingHubClient.normalizeLotNumber("84"));
        assertEquals("0000", BuildingHubClient.normalizeLotNumber(""));
        assertEquals("0000", BuildingHubClient.normalizeLotNumber(null));
    }

    @Test
    @DisplayName("주용도와 기타용도로 실거래가 API용 건물유형을 판별한다")
    void inferBuildingType() {
        assertEquals("APARTMENT", BuildingHubClient.inferBuildingType("공동주택", "주거시설", "은마아파트"));
        assertEquals("MULTI_HOUSEHOLD", BuildingHubClient.inferBuildingType("공동주택", "다세대주택", null));
        assertEquals("MULTI_HOUSEHOLD", BuildingHubClient.inferBuildingType("공동주택", "연립주택", null));
        assertEquals("MULTI_HOUSEHOLD", BuildingHubClient.inferBuildingType("공동주택", "주거시설", "행복빌라"));
        assertEquals("MULTI_FAMILY", BuildingHubClient.inferBuildingType("단독주택", "다가구주택", null));
        assertEquals("SINGLE_FAMILY", BuildingHubClient.inferBuildingType("단독주택", null, null));
        assertEquals("OFFICETEL", BuildingHubClient.inferBuildingType("업무시설", "오피스텔", null));
        assertNull(BuildingHubClient.inferBuildingType("업무시설", "근린생활시설", null));
        assertEquals("MULTI_FAMILY", BuildingHubClient.inferBuildingType(
                "단독주택", "다가구주택", "행복빌라"));
        assertEquals("MULTI_HOUSEHOLD", BuildingHubClient.inferBuildingType(
                "공동주택", "다세대주택", "샹떼빌아파트"));
        assertNull(BuildingHubClient.inferBuildingType(
                "공동주택", "오피스텔, 다세대주택", "행복아파트"));
    }

    @Test
    @DisplayName("위반건축물 값은 Y와 N만 확정하고 나머지는 확인 불가로 둔다")
    void parseIllegalBuildingFlag() {
        assertTrue(BuildingHubClient.parseNullableYn("Y"));
        assertFalse(BuildingHubClient.parseNullableYn("n"));
        assertNull(BuildingHubClient.parseNullableYn(null));
        assertNull(BuildingHubClient.parseNullableYn(""));
        assertNull(BuildingHubClient.parseNullableYn("UNKNOWN"));
    }
}
