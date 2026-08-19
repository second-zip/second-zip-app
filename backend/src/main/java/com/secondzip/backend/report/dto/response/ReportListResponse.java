package com.secondzip.backend.report.dto.response;

import lombok.Getter;
import lombok.AllArgsConstructor;
import java.util.List;

@Getter
@AllArgsConstructor
public class ReportListResponse {
    private List<ReportListItem> reports;
    private int totalCount;
}