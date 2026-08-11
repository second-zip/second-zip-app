package com.secondzip.backend.record.dto.request;

import com.secondzip.backend.record.enums.ChecklistAnalysisStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class RecordingChecklistResultDTO {

    private Long checklistItemId;

    private ChecklistAnalysisStatus status;

    private BigDecimal confidenceScore;

    private String evidenceText;

    private String reason;
}