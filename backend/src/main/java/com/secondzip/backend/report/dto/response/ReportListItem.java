package com.secondzip.backend.report.dto.response;

import com.secondzip.backend.report.enums.RiskLevel;
import lombok.Getter;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReportListItem {
    private Long analysisReportId;
    private String roadAddress;
    private String detailAddress;
    private RiskLevel result;
    private Boolean favorite;
    private LocalDateTime favoritedAt;
    private LocalDateTime createdAt;
}