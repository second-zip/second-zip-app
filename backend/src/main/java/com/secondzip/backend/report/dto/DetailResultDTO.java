package com.secondzip.backend.report.dto;

import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 유형별 세부사항
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetailResultDTO {
    private DetailType detailType;
    private RiskLevel riskLevel;
    private DataStatus dataStatus;

    public DetailResultDTO(DetailType detailType, JudgementDTO judgementDTO) {
        this(detailType, judgementDTO.riskLevel(), judgementDTO.dataStatus());
    }

    public boolean isApplicable() {
        return dataStatus != DataStatus.NOT_APPLICABLE;
    }
}
