package com.secondzip.backend.account.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@ApiModel(description = "비밀번호 변경 요청")
@Getter
@NoArgsConstructor
public class UpdatePasswordDTO {

    @ApiModelProperty(value = "현재 비밀번호", example = "1234", required = true)
    private String currentPassword;

    @ApiModelProperty(value = "새 비밀번호", example = "1234!", required = true)
    private String newPassword;

    @ApiModelProperty(value = "새 비밀번호 확인", example = "1234!", required = true)
    private String newPasswordConfirm;
}