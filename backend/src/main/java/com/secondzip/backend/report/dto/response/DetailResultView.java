package com.secondzip.backend.report.dto.response;

import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DetailResultView {
    private DetailType detailType;
    private RiskLevel result;
    /** 판정에 쓰인 데이터 상태. NOT_APPLICABLE이면 이 매물에 해당하지 않는 항목이다. */
    private DataStatus dataStatus;
}
