package com.secondzip.backend.report.dto.external;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class BuildingRegisterAnalysisData {
    private final BuildingData buildingData;
    private final Long officialPrice;
    private final Map<String, Boolean> violationByDocument;
}
