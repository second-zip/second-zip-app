package com.secondzip.backend.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SpecialTermGenerationResult {

    private List<SpecialTermResult> specialTerms;
}
