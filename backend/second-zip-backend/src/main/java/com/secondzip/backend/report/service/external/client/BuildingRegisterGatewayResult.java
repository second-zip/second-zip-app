package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.CodefTwoWayState;
import com.secondzip.backend.report.dto.AnalysisSelectionOption;
import com.secondzip.backend.report.enums.AnalysisNextAction;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.List;

@Getter
@AllArgsConstructor
public class BuildingRegisterGatewayResult {
    private final boolean completed;
    private final AnalysisNextAction nextAction;
    private final CodefTwoWayState twoWayState;
    private final List<AnalysisSelectionOption> selectionOptions;
    private final String captchaImage;
    private final Map<String, Object> data;
}
