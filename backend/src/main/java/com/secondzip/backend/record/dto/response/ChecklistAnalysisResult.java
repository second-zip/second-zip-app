package com.secondzip.backend.record.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.secondzip.backend.record.enums.ChecklistAnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChecklistAnalysisResult {

    private String summary;

    private List<ResultItem> results;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResultItem {
        private Long checklistItemId;
        private ChecklistAnalysisStatus status;
        private BigDecimal confidenceScore;
        private String evidenceText;
        private String reason;
    }
}