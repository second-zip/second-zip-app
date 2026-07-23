package com.secondzip.backend.account.dto.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {

    @ApiModelProperty(value = "회원 이메일", example = "test@test.com", required = true)
    private String email;

    @ApiModelProperty(value = "회원 비밀번호", example = "1234", required = true)
    private String password;
}