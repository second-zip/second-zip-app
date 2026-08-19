package com.secondzip.backend.map.jeonseprice.controller;

import com.secondzip.backend.map.common.enums.RegionLevel;
import com.secondzip.backend.map.jeonseprice.dto.response.JeonsePriceMapResponse;
import com.secondzip.backend.map.jeonseprice.service.JeonsePriceService;
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
        tags = "지도 API",
        description = "전세사기 피해현황 및 전세가격지수 관련 기능을 제공합니다."
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/maps/jeonse-price")
public class JeonsePriceController {

    private final JeonsePriceService jeonsePriceService;

    @ApiOperation(value = "전세가격지수 지도 조회", notes = "DB에 저장된 시도 또는 시군구 전세가격지수를 조회합니다.")
    @GetMapping
    public ResponseEntity<JeonsePriceMapResponse> getJeonsePrices(
            @ApiParam(value = "지역 단계", example = "SIDO", required = true)
            @RequestParam RegionLevel level,

            @ApiParam(value = "상위 시도 코드. SIGUNGU 조회 시 필수", example = "41")
            @RequestParam(required = false)
            String parentRegionCode
    ) {

        return ResponseEntity.ok(jeonsePriceService.getJeonsePrices(level, parentRegionCode)
        );
    }

}