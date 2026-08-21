package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AnalysisTargetDTO;
import com.secondzip.backend.report.dto.external.PriceData;

import java.math.BigDecimal;

public interface PriceDataProvider {
    PriceData getPriceData(
            AnalysisTargetDTO target,
            String buildingType,
            BigDecimal transactionAreaSqm,
            Integer transactionFloor
    );
}
