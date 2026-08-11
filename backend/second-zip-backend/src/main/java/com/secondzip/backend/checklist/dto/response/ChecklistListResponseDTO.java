package com.secondzip.backend.checklist.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ChecklistListResponseDTO {

    private Long analysisReportId;

    private Long reportChecklistId;

    private String housingCategory;

    private Boolean checklistCreated;

    private LocalDateTime reportCreatedAt;
}