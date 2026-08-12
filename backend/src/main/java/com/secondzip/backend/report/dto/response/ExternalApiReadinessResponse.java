package com.secondzip.backend.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ExternalApiReadinessResponse {
    private final boolean ready;
    private final String codefEnvironment;
    private final List<String> missingConfigurations;
    private final List<String> warnings;
}
