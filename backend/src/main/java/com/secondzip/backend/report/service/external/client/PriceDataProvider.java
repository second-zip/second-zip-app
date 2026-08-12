package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.external.PriceData;

public interface PriceDataProvider {
    PriceData getPriceData(AnalysisTarget target, String buildingType);
}