package com.secondzip.backend.record.enums;

public enum ChecklistAnalysisStatus {
    UNCHECKED,
    PROVISIONAL, // 실시간 분석에서 임시 체크
    CHECKED,
    NEEDS_REVIEW
}