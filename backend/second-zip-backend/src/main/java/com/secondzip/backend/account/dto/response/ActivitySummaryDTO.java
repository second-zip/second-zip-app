package com.secondzip.backend.account.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ActivitySummaryDTO {
    private int totalReportCount;
    private int safeCount;
    private int cautionCount;
    private int dangerCount;
}
