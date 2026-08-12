package com.secondzip.backend.report.dto.request;

import com.secondzip.backend.report.enums.SimpleAuthProvider;
import com.secondzip.backend.report.enums.TelecomProvider;
import lombok.Data;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class StartAnalysisAuthRequest {

    @NotBlank(message = "인증 사용자 이름은 필수입니다.")
    private String userName;

    @NotBlank(message = "생년월일은 필수입니다.")
    @Pattern(regexp = "\\d{8}", message = "생년월일은 yyyyMMdd 8자리여야 합니다.")
    private String birthDate;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "\\d{9,11}", message = "전화번호는 숫자 9~11자리여야 합니다.")
    private String phoneNo;

    @NotNull(message = "간편인증 수단은 필수입니다.")
    private SimpleAuthProvider provider;

    private TelecomProvider telecom;

    @AssertTrue(message = "건축물대장 발급을 위한 본인인증 동의가 필요합니다.")
    private boolean consent;
}
