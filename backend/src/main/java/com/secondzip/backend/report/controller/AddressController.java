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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "주소 검색 API", description = "분석에 사용할 주소 후보를 검색합니다.")
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressSearchService addressSearchService;

    @GetMapping
    @ApiOperation(
            value = "주소 검색 (분석 순서-0)",
            notes = "검색어로 주소 후보 목록을 조회합니다. "
                    + "각 항목의 addressId를 분석 요청에 전달하면 주소를 다시 검색하지 않습니다. "
                    + "검색 결과가 없으면 빈 배열을 반환합니다."
    )
    public ResponseEntity<AddressSearchResponse> search(
            @ApiParam(value = "검색어 (도로명 또는 도로명주소)", required = true, example = "판교역로")
            @RequestParam String query,
            @ApiIgnore Authentication authentication
    ) {
        authenticatedAccountId(authentication);
        return ResponseEntity.ok(addressSearchService.search(query));
    }

    // ReportController와 동일한 방식. SecurityConfig의 인가 규칙과 무관하게 컨트롤러에서 한 번 더 막는다.
    private Long authenticatedAccountId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return (Long) authentication.getPrincipal();
    }
}
