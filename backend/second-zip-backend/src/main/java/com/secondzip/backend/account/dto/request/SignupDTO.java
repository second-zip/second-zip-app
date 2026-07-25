package com.secondzip.backend.account.dto.request;

import com.secondzip.backend.account.enums.CharacterType;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupDTO {
    @ApiModelProperty(value = "로그인에 사용할 이메일", example = "test@test.com", required = true)
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해야 합니다.")
    @ApiModelProperty(value = "로그인 비밀번호", example = "1234", required = true)
    private String password;

    @ApiModelProperty(value = "사용자 닉네임", example = "세컨드집", required = true)
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해야 합니다.")
    private String nickname;

    @ApiModelProperty(value = "캐릭터 유형", example = "MAN", allowableValues = "MAN, WOMAN, CAT", required = true)
    @NotNull(message = "캐릭터 유형은 필수입니다.")
    private CharacterType characterType;
}