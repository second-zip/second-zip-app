package com.secondzip.backend.report.controller;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.response.AddressSearchResponse;
import com.secondzip.backend.report.service.AddressSearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "주소 검색 API", description = "분석 대상 주소를 검색합니다.")
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressSearchService addressSearchService;

    @GetMapping
    @ApiOperation(
            value = "주소 검색",
            notes = "카카오 주소 검색을 백엔드에서 대신 호출합니다. "
                    + "각 결과의 addressToken을 분석 요청에 함께 보내면 "
                    + "사용자가 고른 주소가 그대로 분석 대상이 됩니다."
    )
    public ResponseEntity<AddressSearchResponse> search(
            @ApiParam(value = "검색어", required = true, example = "테헤란로 152")
            @RequestParam String query,
            @ApiParam(value = "페이지 (1부터)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "페이지 크기 (최대 30)", example = "30")
            @RequestParam(defaultValue = "30") int size,
            @ApiIgnore @AuthenticationPrincipal Long accountId
    ) {
        if (accountId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(
                addressSearchService.search(accountId, query, page, size)
        );
    }
}