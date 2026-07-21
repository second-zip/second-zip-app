package com.secondzip.backend.report.controller;

import com.secondzip.backend.report.dto.BuildingData;
import com.secondzip.backend.report.dto.PriceData;
import com.secondzip.backend.report.dto.RegistryData;
import com.secondzip.backend.report.service.external.ExternalDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class ExternalDataTestController {

    // 우리가 만든 MockExternalDataService가 주입됩니다.
    private final ExternalDataService externalDataService;

    @GetMapping("/external")
    public ResponseEntity<Map<String, Object>> testExternalData(@RequestParam String address) {

        // 1. 각 DTO에 데이터 채우기 (Mock 서비스 호출)
        RegistryData registryData = externalDataService.getRegistryData(address);
        BuildingData buildingData = externalDataService.getBuildingData(address);
        PriceData priceData = externalDataService.getPriceData(address);

        // 2. 결과를 한 번에 볼 수 있게 Map에 담기
        Map<String, Object> result = new HashMap<>();
        result.put("address", address);
        result.put("registryData", registryData);
        result.put("buildingData", buildingData);
        result.put("priceData", priceData);

        // 3. JSON 형태로 응답 반환
        return ResponseEntity.ok(result);
    }
}