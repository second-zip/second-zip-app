package com.secondzip.backend.map.jeonseprice.controller;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.map.jeonseprice.dto.response.JeonsePriceSyncResponse;
import com.secondzip.backend.map.jeonseprice.service.JeonsePriceSyncService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
public class JeonsePriceSyncController {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final JeonsePriceSyncService jeonsePriceSyncService;

    @ApiOperation(value = "전세가격지수 데이터 동기화", notes = "한국부동산원에서 기준월과 전월의 데이터를 조회해 DB에 저장합니다.")
    @PostMapping("/sync")
    public ResponseEntity<JeonsePriceSyncResponse> sync(
            @ApiParam(value = "동기화할 기준월", required = true, example = "202606")
            @RequestParam String month
    ) {
        YearMonth targetMonth = parseMonth(month);

        int savedCount = jeonsePriceSyncService.sync(targetMonth);

        JeonsePriceSyncResponse response =
                JeonsePriceSyncResponse.of(
                        targetMonth.format(MONTH_FORMATTER),
                        savedCount
                );

        return ResponseEntity.ok(response);
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(
                    month,
                    MONTH_FORMATTER
            );

        } catch (DateTimeParseException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "month는 yyyyMM 형식이어야 합니다. 예: 202606"
            );
        }
    }
}
