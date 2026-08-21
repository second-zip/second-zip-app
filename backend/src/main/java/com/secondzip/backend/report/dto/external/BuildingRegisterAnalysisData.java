package com.secondzip.backend.report.dto.external;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@AllArgsConstructor
public class BuildingRegisterAnalysisData {
    private final BuildingData buildingData;
    private final Long officialPrice;
    private final Map<String, Boolean> violationByDocument;
    /** 집합건물은 대상 전유부 전용면적, 일반건물은 전체 연면적(㎡). */
    private final BigDecimal transactionAreaSqm;
    /** 대상 전유부 장부상 층. 일반건물 또는 장부값 미확인 시 null. */
    private final Integer transactionFloor;

    /** 면적·층 필드 추가 전 호출부와 테스트의 단계적 이행을 위한 호환 생성자. */
    public BuildingRegisterAnalysisData(
            BuildingData buildingData,
            Long officialPrice,
            Map<String, Boolean> violationByDocument
    ) {
        this(buildingData, officialPrice, violationByDocument, null, null);
    }
}
