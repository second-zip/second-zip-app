package com.secondzip.backend.account.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@ApiModel(description = "비밀번호 변경 요청")
@Getter
@NoArgsConstructor
public class UpdatePasswordDTO {

    @ApiModelProperty(value = "현재 비밀번호", example = "password1!", required = true)
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 16자 이하로 입력해야 합니다.")
    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    private String currentPassword;

    @ApiModelProperty(value = "새 비밀번호", example = "password1!", required = true)
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 16자 이하로 입력해야 합니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다"
    )
    @NotBlank(message = "새 비밀번호는 필수입니다.")
    private String newPassword;

    @ApiModelProperty(value = "새 비밀번호 확인", example = "password1!", required = true)
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 16자 이하로 입력해야 합니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다"
    )
    @NotBlank(message = "새 비밀번호 확인은 필수입니다.")
    private String newPasswordConfirm;
}