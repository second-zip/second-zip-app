package com.secondzip.backend.account.dto.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenReissueRequestDTO {

    @ApiModelProperty(
            value = "Refresh Token",
            required = true
    )
    @NotBlank(message = "Refresh Token은 필수입니다.")
    private String refreshToken;
}