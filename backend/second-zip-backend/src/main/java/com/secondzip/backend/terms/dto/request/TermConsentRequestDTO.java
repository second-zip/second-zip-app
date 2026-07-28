package com.secondzip.backend.terms.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
public class TermConsentRequestDTO {

    @NotNull
    private Long termId;

    @NotNull
    private Boolean agreed;
}