package com.secondzip.backend.report.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
@ApiModel(description = "분석 보고서 생성 요청")
public class CreateReportRequest {

    @ApiModelProperty(value = "도로명 주소", required = true, example = "서울특별시 강남구 테헤란로 1")
    @NotBlank(message = "도로명 주소는 필수입니다.")
    private String roadAddress;

    @ApiModelProperty(value = "상세 주소", example = "101동 101호")
    private String detailAddress;

    @ApiModelProperty(value = "전세 보증금 (원 단위)", required = true, example = "100000000")
    @NotNull(message = "전세 보증금은 필수입니다.")
    @Positive(message = "전세 보증금은 0보다 커야 합니다.")
    private Long deposit;

    @ApiModelProperty(value = "주소 검색에서 받은 법정동코드 10자리", example = "4113511000")
    private String legalDongCode;
}
