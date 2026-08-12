package com.secondzip.backend.checklist.dto.response;

import com.secondzip.backend.checklist.enums.Category;
import com.secondzip.backend.report.enums.RiskLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ChecklistListResponseDTO {

    private String roadAddress;

    private String detailAddress;

    private Long analysisReportId;

    private Long reportChecklistId;

    private Category housingCategory;

    private Boolean checklistCreated;

    private LocalDateTime reportCreatedAt;

    private Boolean trustProperty;

    private RiskLevel riskLevel;

    private Integer checkedCount;

    private Integer totalCount;

    private Integer progressPercentage;
}