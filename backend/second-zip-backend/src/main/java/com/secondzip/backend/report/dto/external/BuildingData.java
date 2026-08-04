package com.secondzip.backend.report.dto.external;

import lombok.Data;

import java.util.Map;

@Data
public class BuildingData {
    private Boolean isIllegalBuilding;
    private Boolean illegalBuildingVerified;
    private String illegalBuildingSource;
    private Map<String, Boolean> violationByDocument;
    private String buildingUse;
    private String buildingType;
}
