package com.secondzip.backend.checklist.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportChecklist {

    private Long reportChecklistId;

    private Long analysisReportId;

    private Long accountId;
}