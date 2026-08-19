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

    @ApiModelProperty(
            value = "주소 검색(GET /api/addresses)에서 받은 주소 식별자",
            required = true,
            example = "b7f3c2a1-4d5e-4f6a-8b9c-0d1e2f3a4b5c"
    )
    @NotBlank(message = "주소를 선택해주세요.")
    private String addressId;

    @ApiModelProperty(value = "상세 주소", example = "101동 101호")
    private String detailAddress;

    @ApiModelProperty(value = "전세 보증금 (원 단위)", required = true, example = "100000000")
    @NotNull(message = "전세 보증금은 필수입니다.")
    @Positive(message = "전세 보증금은 0보다 커야 합니다.")
    private Long deposit;
}
