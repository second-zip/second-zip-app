package com.secondzip.backend.terms.dto.response;

import com.secondzip.backend.terms.enums.TermType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@ApiModel(description = "회원 약관 동의 상태 응답")
@Getter
@Builder
public class TermConsentResponseDTO {

    @ApiModelProperty(value = "약관 식별자", example = "1")
    private Long termId;

    @ApiModelProperty(value = "약관 제목", example = "마케팅 정보 수신 동의")
    private String title;

    @ApiModelProperty(
            value = "약관 종류",
            example = "MARKETING",
            allowableValues = "SERVICE, PRIVACY_POLICY, MARKETING"
    )
    private TermType termType;

    @ApiModelProperty(value = "필수 약관 여부", example = "false")
    private Boolean required;

    @ApiModelProperty(value = "약관 버전", example = "1.0")
    private String version;

    @ApiModelProperty(value = "회원 동의 여부", example = "true")
    private Boolean agreed;

    @ApiModelProperty(value = "동의 일시", example = "2026-07-28T12:10:00")
    private LocalDateTime agreedAt;
}