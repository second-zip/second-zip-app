package com.secondzip.backend.report.dto.response;

import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;


@Getter
@AllArgsConstructor
public class CheckResultView {
    private CheckType checkType;
    private RiskLevel result;
    private Map<String, Object> evidence;   // 필수 항목 판단 데이터 저장
}