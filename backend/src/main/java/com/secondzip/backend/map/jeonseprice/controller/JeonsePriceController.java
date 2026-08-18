package com.secondzip.backend.map.jeonseprice.controller;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
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

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Api(
        tags = "지도 API",
        description = "전세사기 피해현황 및 전세가격지수 관련 기능을 제공합니다."
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/maps/jeonse-price")
public class JeonsePriceController {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final JeonsePriceService jeonsePriceService;

    @ApiOperation(value = "전세가격지수 지도 조회", notes = "DB에 저장된 시도 또는 시군구 전세가격지수를 조회합니다.")
    @GetMapping
    public ResponseEntity<JeonsePriceMapResponse> getJeonsePrices(
            @ApiParam(value = "조회 기준월", example = "202606", required = true)
            @RequestParam String month,

            @ApiParam(value = "지역 단계", example = "SIDO", required = true)
            @RequestParam RegionLevel level,

            @ApiParam(value = "상위 시도 코드. SIGUNGU 조회 시 필수", example = "41")
            @RequestParam(required = false)
            String parentRegionCode
    ) {
        YearMonth baseMonth = parseMonth(month);

        return ResponseEntity.ok(jeonsePriceService.getJeonsePrices(baseMonth, level, parentRegionCode)
        );
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month, MONTH_FORMATTER);

        } catch (DateTimeParseException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "month는 yyyyMM 형식이어야 합니다. 예: 202606"
            );
        }
    }
}