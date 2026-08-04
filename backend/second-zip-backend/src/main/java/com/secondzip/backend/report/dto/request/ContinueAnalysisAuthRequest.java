package com.secondzip.backend.report.dto.request;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
public class ContinueAnalysisAuthRequest {

    @Valid
    @NotNull(message = "간편인증 정보는 필수입니다.")
    private StartAnalysisAuthRequest authentication;

    private String selectionValue;
    private String secureNo;
}
