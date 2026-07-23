package com.secondzip.backend.account.dto.request;

import com.secondzip.backend.account.enums.CharacterType;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupDTO {
    @ApiModelProperty(value = "로그인에 사용할 이메일", example = "test@test.com", required = true)
    private String email;

    @ApiModelProperty(value = "로그인 비밀번호", example = "1234", required = true)
    private String password;

    @ApiModelProperty(value = "사용자 닉네임", example = "세컨드집", required = true)
    private String nickname;

    @ApiModelProperty(value = "캐릭터 유형", example = "MAN", allowableValues = "MSN, WOMAN, CAT", required = true)
    private CharacterType characterType;
}