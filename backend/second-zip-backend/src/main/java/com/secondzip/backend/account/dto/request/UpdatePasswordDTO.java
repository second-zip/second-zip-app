package com.secondzip.backend.account.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

@ApiModel(description = "비밀번호 변경 요청")
@Getter
@NoArgsConstructor
public class UpdatePasswordDTO {

    @ApiModelProperty(value = "현재 비밀번호", example = "1234", required = true)
    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    private String currentPassword;

    @ApiModelProperty(value = "새 비밀번호", example = "1234!", required = true)
    @NotBlank(message = "새 비밀번호는 필수입니다.")
    private String newPassword;

    @ApiModelProperty(value = "새 비밀번호 확인", example = "1234!", required = true)
    @NotBlank(message = "새 비밀번호 확인은 필수입니다.")
    private String newPasswordConfirm;
}