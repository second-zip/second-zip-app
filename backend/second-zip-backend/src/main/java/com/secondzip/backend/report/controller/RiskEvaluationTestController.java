package com.secondzip.backend.report.controller;

import com.secondzip.backend.report.dto.RiskEvaluationResult;
import com.secondzip.backend.report.service.RiskEvaluationService;
import com.secondzip.backend.report.service.external.ExternalDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RiskEvaluationTestController {

    private final ExternalDataService externalDataService;
    private final RiskEvaluationService riskEvaluationService;

    @GetMapping("/api/test/evaluate")
    public RiskEvaluationResult test(@RequestParam String address, @RequestParam Long deposit) {
        var registry = externalDataService.getRegistryData(address);
        var building = externalDataService.getBuildingData(address);
        var price = externalDataService.getPriceData(address);
        return riskEvaluationService.evaluate(registry, building, price, deposit, address);
    }
}