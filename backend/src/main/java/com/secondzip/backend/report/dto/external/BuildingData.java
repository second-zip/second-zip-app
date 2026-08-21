package com.secondzip.backend.report.dto.external;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class BuildingData {
    private Boolean isIllegalBuilding;
    private Boolean illegalBuildingVerified;
    private String illegalBuildingSource;
    private Map<String, Boolean> violationByDocument;
    /** 계약 대상 호(집합건물은 전유부, 단독·다가구는 일반건축물)의 용도. */
    private String buildingUse;
    /**
     * 표제부에만 나타나는 비주거 용도. 예: 1층이 근린생활시설인 다세대주택.
     *
     * {@link #buildingUse}에 합쳐 적으면 계약 대상 호 자체가 비주거인 것처럼
     * 읽혀 정상 매물이 DANGER로 판정된다. 반드시 분리해 보관한다.
     */
    private String buildingLevelNonResidentialUses;
    private String buildingType;
    /** 실거래가 후보 매칭에 쓰는 면적(㎡). HUB에서는 선택된 표제부의 연면적. */
    private BigDecimal transactionAreaSqm;
}
