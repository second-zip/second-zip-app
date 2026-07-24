package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.BuildingData;
import com.secondzip.backend.report.dto.PriceData;
import com.secondzip.backend.report.dto.RegistryData;
import com.secondzip.backend.report.dto.RiskEvaluationResult;
import com.secondzip.backend.report.dto.request.CreateReportRequest;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.service.external.ExternalDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ExternalDataService externalDataService;
    private final RiskEvaluationService riskEvaluationService;
    private final ReportPersistenceService reportPersistenceService;

        // 분석 담당 (조회, 판정, 저장)
    public ReportDetailResponse createReport(Long accountId, CreateReportRequest request) {
        // 조회
        RegistryData registry = externalDataService.getRegistryData(request.getRoadAddress());
        BuildingData building = externalDataService.getBuildingData(request.getRoadAddress());
        PriceData price = externalDataService.getPriceData(request.getRoadAddress());

        // 가져온 데이터로 위험분석결과 로직 수행 (판정)
        RiskEvaluationResult evalResult = riskEvaluationService.evaluate(
                registry, building, price, request.getDeposit(), request.getRoadAddress()
        );

        // 저장
        return reportPersistenceService.save(
                accountId, request.getRoadAddress(), request.getDetailAddress(),
                request.getDeposit(), evalResult
        );
    }
}