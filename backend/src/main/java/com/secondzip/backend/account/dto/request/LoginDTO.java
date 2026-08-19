package com.secondzip.backend.account.dto.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

import javax.validation.constraints.Email;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {

    @ApiModelProperty(value = "회원 이메일", example = "test@test.com", required = true)
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @ApiModelProperty(value = "회원 비밀번호", example = "password1!", required = true)
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}