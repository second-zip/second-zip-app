package com.secondzip.backend.terms.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@ApiModel(description = "약관 동의 상태 변경 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTermConsentDTO {

    @ApiModelProperty(
            value = "약관 동의 여부",
            example = "true",
            required = true
    )
    @NotNull(message = "약관 동의 여부는 필수입니다.")
    private Boolean agreed;
}