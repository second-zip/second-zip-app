package com.secondzip.backend.account.dto.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

import javax.validation.constraints.Size;

@Getter
@NoArgsConstructor
public class UpdateAccountDTO {
    @ApiModelProperty(value = "변경할 닉네임", example = "세컨드집", required = true)
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해야 합니다.")
    private String nickname;
}