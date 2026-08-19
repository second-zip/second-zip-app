package com.secondzip.backend.report.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ApiModel(description = "주소 검색 결과 항목")
public class AddressSearchItem {

    @ApiModelProperty(
            value = "분석 요청 시 전달할 주소 식별자. 30분간 유효하다.",
            example = "b7f3c2a1-4d5e-4f6a-8b9c-0d1e2f3a4b5c"
    )
    private String addressId;

    @ApiModelProperty(
            value = "도로명주소. 도로명이 부여되지 않은 주소는 빈 값일 수 있다.",
            example = "경기 성남시 분당구 판교역로 235"
    )
    private String roadAddress;

    @ApiModelProperty(value = "지번주소", example = "경기 성남시 분당구 삼평동 681")
    private String jibunAddress;

    @ApiModelProperty(value = "우편번호. 없을 수 있다.", example = "13494")
    private String zoneNo;

    @ApiModelProperty(
            value = "건물·장소명. 건물명으로 검색된 경우에만 채워진다.",
            example = "판교테크노밸리"
    )
    private String placeName;
}
