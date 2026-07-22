package com.secondzip.backend.report.dto;


import lombok.Getter;

@Getter
public class BuildingData {
    private Boolean isIllegalBuilding;  // 위반건축물 등재 여부
    private String buildingUse;         // 건축물 용도 (공동주택, 근린생활시설 등)
    private String buildingType;        // SINGLE_FAMILY / MULTI_FAMILY / APARTMENT / MULTI_HOUSEHOLD / OFFICETEL
}