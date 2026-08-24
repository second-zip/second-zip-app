package com.secondzip.backend.terms.dto.response;

import com.secondzip.backend.terms.domain.Term;
import com.secondzip.backend.terms.enums.TermType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@ApiModel(description = "약관 조회 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermResponseDTO {

    @ApiModelProperty(value = "약관 식별자", example = "1")
    private Long termId;

    @ApiModelProperty(value = "약관 제목", example = "서비스 이용약관")
    private String title;

    @ApiModelProperty(value = "약관 내용", example = "제1조 목적...")
    private String content;

    @ApiModelProperty(
            value = "약관 종류",
            example = "SERVICE",
            allowableValues = "SERVICE, PRIVACY, MARKETING"
    )
    private TermType termType;

    @ApiModelProperty(value = "필수 동의 여부", example = "true")
    private Boolean required;

    @ApiModelProperty(value = "약관 버전", example = "1.0")
    private String version;

    public static TermResponseDTO from(Term term) {
        return TermResponseDTO.builder()
                .termId(term.getTermId())
                .title(term.getTitle())
                .content(term.getContent())
                .termType(term.getTermType())
                .required(term.getRequired())
                .version(term.getVersion())
                .build();
    }
}