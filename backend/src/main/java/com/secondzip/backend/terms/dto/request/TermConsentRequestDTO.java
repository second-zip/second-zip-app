package com.secondzip.backend.terms.dto.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
public class TermConsentRequestDTO {

    @ApiModelProperty(
            value = "약관 ID",
            example = "1",
            required = true
    )
    @NotNull
    private Long termId;

    @ApiModelProperty(
            value = "동의 여부",
            example = "true",
            required = true
    )
    @NotNull
    private Boolean agreed;
}