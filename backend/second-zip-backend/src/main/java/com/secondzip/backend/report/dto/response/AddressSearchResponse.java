package com.secondzip.backend.report.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 주소 검색 응답.
 * 카카오 응답 구조(documents / road_address …)를 노출 X
 * 나중에 다른 주소 제공자로 바꿀 때 프론트까지 같이 고쳐야 하기 때문
 */
@Getter
@AllArgsConstructor
@ApiModel(description = "주소 검색 결과")
public class AddressSearchResponse {

    @ApiModelProperty(value = "검색 결과 목록")
    private List<AddressItem> addresses;

    @Getter
    @AllArgsConstructor
    @ApiModel(description = "주소 검색 결과 한 건")
    public static class AddressItem {

        @ApiModelProperty(
                value = "분석 요청 시 함께 보낼 토큰. 10분간 유효",
                example = "3f2b1c9e-..."
        )
        private String addressToken;

        @ApiModelProperty(value = "도로명 주소", example = "서울 강남구 테헤란로 152")
        private String roadAddress;

        @ApiModelProperty(value = "지번 주소", example = "서울 강남구 역삼동 737")
        private String jibunAddress;

        @ApiModelProperty(value = "우편번호", example = "06236")
        private String zoneNo;
    }
}