package com.secondzip.backend.report.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "분석 보고서 생성 요청")
public class CreateReportRequest {

    @ApiModelProperty(value = "도로명 주소", required = true, example = "서울특별시 강남구 테헤란로 1")
    private String roadAddress;

    @ApiModelProperty(value = "상세 주소", example = "101동 101호")
    private String detailAddress;

    @ApiModelProperty(value = "전세 보증금 (원 단위)", required = true, example = "100000000")
    private Long deposit;
}