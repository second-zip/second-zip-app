package com.secondzip.backend.account.dto.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

@Getter
@NoArgsConstructor
public class WithdrawAccountDTO {

    @ApiModelProperty(value = "회원 탈퇴 확인을 위한 현재 비밀번호", example = "password1!", required = true)
    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    private String password;
}