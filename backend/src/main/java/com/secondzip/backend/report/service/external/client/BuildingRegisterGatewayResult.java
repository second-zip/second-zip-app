package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.CodefTwoWayStateDTO;
import com.secondzip.backend.report.dto.AnalysisSelectionOptionDTO;
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
    private final CodefTwoWayStateDTO twoWayState;
    private final List<AnalysisSelectionOptionDTO> selectionOptions;
    private final String captchaImage;
    private final Map<String, Object> data;
}
