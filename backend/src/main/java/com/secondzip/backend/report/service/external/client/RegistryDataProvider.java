package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AnalysisTargetDTO;
import com.secondzip.backend.report.dto.external.RegistryData;

// mock 데이터 전용
public interface RegistryDataProvider {
    RegistryData getRegistryDataForAnalysis(
            AnalysisTargetDTO target,
            String detailAddress,
            String buildingType
    );
}