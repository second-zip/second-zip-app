package com.secondzip.backend.account.dto.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WithdrawAccountDTO {

    @ApiModelProperty(value = "회원 탈퇴 확인을 위한 현재 비밀번호", example = "password123", required = true)
    private String password;
}