package com.secondzip.backend.map.frauddamage.controller;

import com.secondzip.backend.map.common.enums.RegionLevel;
import com.secondzip.backend.map.frauddamage.dto.response.FraudDamageMapResponse;
import com.secondzip.backend.map.frauddamage.service.FraudDamageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(
        tags = "지도 - 피해현황 조회 API",
        description = "시도 및 시군구별 전세사기 피해주택 현황을 조회합니다."
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/maps")
public class FraudDamageController {
    private final FraudDamageService fraudDamageService;

    @ApiOperation(
            value = "전세사기 피해현황 지도 조회",
            notes = "시도별 또는 선택한 시도의 시군구별 전세사기 피해현황을 조회합니다."
    )
    @GetMapping("/fraud-damage")
    public ResponseEntity<FraudDamageMapResponse> getFraudDamages(
            @ApiParam(
                    value = "행정구역 단계: SIDO 또는 SIGUNGU",
                    required = true,
                    example = "SIDO"
            )
            @RequestParam(name = "level")
            RegionLevel regionLevel,

            @ApiParam(value = "상위 시도 지역 코드. level이 SIGUNGU일 때 필수", example = "11")
            @RequestParam(name = "parentRegionCode", required = false)
            String parentRegionCode
    ){
        return ResponseEntity.ok(
                fraudDamageService.getFraudDamages(
                        regionLevel, parentRegionCode
                )
        );

    }
}
