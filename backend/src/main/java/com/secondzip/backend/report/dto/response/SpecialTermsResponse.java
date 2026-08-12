package com.secondzip.backend.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SpecialTermsResponse {

    private List<SpecialTermView> specialTerms;
}
