package com.secondzip.backend.report.dto.response;

import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;


@Getter
@AllArgsConstructor
public class CheckResultView {
    private CheckType checkType;
    private RiskLevel result;
    /** 판정에 쓰인 데이터 상태. CAUTION이 "위험"인지 "확인 불가"인지 구분하는 데 쓴다. */
    private DataStatus dataStatus;
    private Map<String, Object> evidence;   // 필수 항목 판단 데이터 저장
}
